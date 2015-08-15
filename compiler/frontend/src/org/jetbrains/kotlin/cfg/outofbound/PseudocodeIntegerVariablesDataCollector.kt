/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cfg.outofbound

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.JetNodeType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.LexicalScopeVariableInfo
import org.jetbrains.kotlin.cfg.LexicalScopeVariableInfoImpl
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.NondeterministicJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.UnconditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.collectData
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.HashMap
import java.util.HashSet

// This file contains functionality similar to org.jetbrains.kotlin.cfg.PseudocodeVariablesData,
// but collects information about integer variables' values. Semantically it would be better to
// merge functionality in this two files

public class PseudocodeIntegerVariablesDataCollector(val pseudocode: Pseudocode, val bindingContext: BindingContext) {
    private val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)
    // This map contains mapping from instruction to flag indicating the instruction is or is not inside some loop
    // This info is used to correctly process update operations inside loops
    private val loopsInfoMap: HashMap<Instruction, Boolean> = createLoopsInfoMap()

    // this function is fully copied from PseudocodeVariableDataCollector
    private fun computeLexicalScopeVariableInfo(pseudocode: Pseudocode): LexicalScopeVariableInfo {
        val lexicalScopeVariableInfo = LexicalScopeVariableInfoImpl()
        pseudocode.traverse(TraversalOrder.FORWARD, { instruction ->
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.variableDeclarationElement
                val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                if (descriptor != null) {
                    // TODO: investigate why tests fail without this eager computation here
                    descriptor.toString()

                    assert(descriptor is VariableDescriptor) {
                        "Variable descriptor should correspond to the instruction for ${instruction.element.getText()}.\n" +
                        "Descriptor: $descriptor"
                    }
                    lexicalScopeVariableInfo.registerVariableDeclaredInScope(
                            descriptor as VariableDescriptor, instruction.lexicalScope
                    )
                }
            }
        })
        return lexicalScopeVariableInfo
    }

    private fun createLoopsInfoMap(): HashMap<Instruction, Boolean> {
        if (pseudocode !is PseudocodeImpl) {
            throw IllegalArgumentException("Pseudocode is not a PseudocodeImpl so there is no labels info")
        }
        val labels = pseudocode.labels
        val instructionIndexToLabels = HashMap<Int, MutableSet<PseudocodeImpl.PseudocodeLabel>>(labels.size() + 5, 1f) // + 5 is chosen randomly
        labels.forEach { instructionIndexToLabels.getOrPut(it.targetInstructionIndex, { HashSet() }).add(it) }
        val instructions = pseudocode.instructionsIncludingDeadCode
        val loopInfoMap = HashMap<Instruction, Boolean>(instructions.size() + 5, 1f)
        var inLoop = false
        instructions.forEachIndexed { i, instruction ->
            val instructionLabels = instructionIndexToLabels[i]
            if (instructionLabels != null) {
                val labelsAsStrings = instructionLabels.map { it.toString() }
                if (!inLoop) {
                    val isWhileLoopEnter = labelsAsStrings.any { it.contains("loop entry point") } &&
                                           (instruction !is NondeterministicJumpInstruction || instruction.element !is JetForExpression)
                    val isForLoopBodyEnter = labelsAsStrings.any { it.contains("body entry point") }
                    inLoop = isWhileLoopEnter || isForLoopBodyEnter
                }
                else {
                    inLoop = !labelsAsStrings.any { it.contains("loop exit point") }
                }
            }
            else {
                loopInfoMap[instruction] = inLoop
            }
            loopInfoMap[instruction] = inLoop
        }
        return loopInfoMap
    }

    public fun collectVariableValuesData(): Map<Instruction, Edges<ValuesData>> {
        return pseudocode.collectData(
                TraversalOrder.FORWARD,
                /* mergeDataWithLocalDeclarations */ true,
                { instruction, incomingData: Collection<ValuesData> -> mergeVariablesValues(instruction, incomingData) },
                { previous, current, edgeData -> updateEdge(previous, current, edgeData) },
                ValuesData()
        )
    }

    private fun updateEdge(previousInstruction: Instruction, currentInstruction: Instruction, edgeData: ValuesData): ValuesData {
        val updatedEdgeData = edgeData.copy()
        val filteredEdgeData = removeOutOfScopeVariables(previousInstruction, currentInstruction, updatedEdgeData)
        applyRestrictionsOnValuesIfNeeded(previousInstruction, currentInstruction, filteredEdgeData)
        return filteredEdgeData
    }

    private fun removeOutOfScopeVariables(
            previousInstruction: Instruction,
            currentInstruction: Instruction,
            edgeData: ValuesData
    ): ValuesData {
        val filteredIntVars = filterOutVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.intVarsToValues)
        val filteredIntFakeVars = filterOutFakeVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.intFakeVarsToValues)
        val filteredBoolVars = filterOutVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.boolVarsToValues)
        val filteredBoolFakeVars = filterOutFakeVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.boolFakeVarsToValues)
        val filteredArrayVars = filterOutVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.collectionsToSizes)
        return ValuesData(
                HashMap(filteredIntVars),
                HashMap(filteredIntFakeVars),
                HashMap(filteredBoolVars),
                HashMap(filteredBoolFakeVars),
                HashMap(filteredArrayVars)
        )
    }

    // this function is fully copied from PseudocodeVariableDataCollector
    private fun <D> filterOutVariablesOutOfScope(
            from: Instruction,
            to: Instruction,
            data: Map<VariableDescriptor, D>
    ): Map<VariableDescriptor, D> {
        // If an edge goes from deeper lexical scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.lexicalScope.depth
        if (toDepth >= from.lexicalScope.depth) return data

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        return data.filterKeys { variable ->
            val lexicalScope = lexicalScopeVariableInfo.declaredIn[variable]
            // '-1' for variables declared outside this pseudocode
            val depth = lexicalScope?.depth ?: -1
            depth <= toDepth
        }
    }

    // This method is much like the previous one. The generalization
    // must be done after merge with PseudocodeVariableDataCollector
    private fun <D> filterOutFakeVariablesOutOfScope(
            from: Instruction,
            to: Instruction,
            data: Map<PseudoValue, D>
    ): Map<PseudoValue, D> {
        // If an edge goes from deeper lexical scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.lexicalScope.depth
        if (toDepth >= from.lexicalScope.depth) return data

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        return data.filterKeys { variable ->
            val lexicalScope = variable.createdAt?.lexicalScope
            // '-1' for variables declared outside this pseudocode
            val depth = lexicalScope?.depth ?: -1
            depth <= toDepth
        }
    }

    private fun applyRestrictionsOnValuesIfNeeded(
            previousInstruction: Instruction,
            currentInstruction: Instruction,
            edgeData: ValuesData
    ) {
        if (previousInstruction is ConditionalJumpInstruction && previousInstruction.element !is JetBinaryExpression) {
            val conditionBoolValue = edgeData.boolFakeVarsToValues.remove(previousInstruction.conditionValue)
            if (conditionBoolValue != null && previousInstruction.element is JetIfExpression) {
                applyRestrictionsOnValues(conditionBoolValue, currentInstruction, previousInstruction.nextOnTrue,
                                          previousInstruction.nextOnFalse, edgeData)
            }
        }
        if (previousInstruction is NondeterministicJumpInstruction &&
            previousInstruction.element is JetWhenExpression &&
            previousInstruction.inputValue != null
        ) {
            val nextOnTrue = previousInstruction.next
            val nextOnFalse =
                    if (previousInstruction.resolvedTargets.size() > 0)
                        previousInstruction.resolvedTargets.values().first()
                    else return
            val conditionBoolValue = edgeData.boolFakeVarsToValues.remove(previousInstruction.inputValue)
            if (conditionBoolValue != null) {
                applyRestrictionsOnValues(conditionBoolValue, currentInstruction, nextOnTrue, nextOnFalse, edgeData)
            }
        }
    }

    private fun applyRestrictionsOnValues(
            conditionBoolValue: BooleanVariableValue,
            currentInstruction: Instruction,
            onTrueInstruction: Instruction,
            onFalseInstruction: Instruction,
            edgeData: ValuesData) {
        when (conditionBoolValue) {
            is BooleanVariableValue.True -> {
                if (onFalseInstruction == currentInstruction) {
                    // We are in "else" block and condition evaluated to "true" so this block will not
                    // be processed (dead code block). To indicate this we will make all variables dead
                    edgeData.intVarsToValues.entrySet().forEach { it.setValue(IntegerVariableValues.Dead) }
                }
            }
            is BooleanVariableValue.False -> {
                if (onTrueInstruction == currentInstruction) {
                    // We are in "then" block and condition evaluated to "false" so this block will not
                    // be processed (dead code block). To indicate this we will make all variables dead
                    edgeData.intVarsToValues.entrySet().forEach { it.setValue(IntegerVariableValues.Dead) }
                }
            }
            is BooleanVariableValue.Undefined -> {
                val restrictions =
                        if (onTrueInstruction == currentInstruction) {
                            // We are in "then" block and need to apply onTrue restrictions
                            conditionBoolValue.onTrueRestrictions
                        }
                        else {
                            assert(onFalseInstruction == currentInstruction)
                            // We are in "else" block and need to apply onFalse restrictions
                            conditionBoolValue.onFalseRestrictions
                        }
                for ((variable, unrestrictedValues) in restrictions) {
                    val values = edgeData.intVarsToValues[variable]
                    if (values is IntegerVariableValues.Defined) {
                        edgeData.intVarsToValues[variable] = values.leaveOnlyValuesInSet(unrestrictedValues)
                    }
                }
            }
        }
    }

    private fun mergeVariablesValues(instruction: Instruction, incomingEdgesData: Collection<ValuesData>): Edges<ValuesData> {
        if (instruction is SubroutineSinkInstruction) {
            // this instruction is assumed to be the last one in function so it is not processed
            return Edges(ValuesData(), ValuesData())
        }
        val enterInstructionData = unionIncomingVariablesValues(incomingEdgesData)
        val exitInstructionData = updateValues(instruction, enterInstructionData)
        return Edges(enterInstructionData, exitInstructionData)
    }

    private fun unionIncomingVariablesValues(incomingEdgesData: Collection<ValuesData>): ValuesData {
        if (incomingEdgesData.isEmpty()) {
            return ValuesData()
        }
        val headData = incomingEdgesData.first()
        val tailData = incomingEdgesData.drop(1)
        val unitedIntVariables = headData.intVarsToValues
        val unitedIntFakeVariables = headData.intFakeVarsToValues
        val unitedBoolVariables = headData.boolVarsToValues
        val unitedBoolFakeVariables = headData.boolFakeVarsToValues
        val unitedArrayVariables = headData.collectionsToSizes
        for (data in tailData) {
            mergeCorrespondingIntegerVariables(unitedIntVariables, data.intVarsToValues)
            assert(unitedIntFakeVariables.all { data.intFakeVarsToValues[it.key]?.equals(it.value) ?: true },
                   "intFake variables assumption failed")
//            if(!(unitedIntFakeVariables.all { data.intFakeVarsToValues[it.key]?.equals(it.value) ?: true })) {
//                Unit
//            }
            unitedIntFakeVariables.putAll(data.intFakeVarsToValues)
            MapUtils.mergeMapsIntoFirst(unitedBoolVariables, data.boolVarsToValues) { value1, value2 -> value1.or(value2) }
            assert(unitedBoolFakeVariables.all { data.boolFakeVarsToValues[it.key]?.equals(it.value) ?: true },
                   "boolFake variables assumption failed")
//            if(!(unitedBoolFakeVariables.all { data.boolFakeVarsToValues[it.key]?.equals(it.value) ?: true })) {
//                Unit
//            }
            unitedBoolFakeVariables.putAll(data.boolFakeVarsToValues)
            mergeCorrespondingIntegerVariables(unitedArrayVariables, data.collectionsToSizes)
        }
        return ValuesData(unitedIntVariables, unitedIntFakeVariables, unitedBoolVariables, unitedBoolFakeVariables, unitedArrayVariables)
    }

    private fun mergeCorrespondingIntegerVariables<K>(
            targetVariablesMap: MutableMap<K, IntegerVariableValues>,
            variablesToConsume: MutableMap<K, IntegerVariableValues>
    ) = MapUtils.mergeMapsIntoFirst(targetVariablesMap, variablesToConsume) { value1, value2 -> value1 merge value2 }

    private fun updateValues(instruction: Instruction, mergedEdgesData: ValuesData): ValuesData {
        val updatedData = mergedEdgesData.copy()
        when(instruction) {
            is VariableDeclarationInstruction -> processVariableDeclaration(instruction, updatedData)
            is ReadValueInstruction -> {
                val element = instruction.outputValue.element
                when (element) {
                    is JetConstantExpression -> processLiteral(element, instruction, updatedData)
                    is JetNameReferenceExpression -> processVariableReference(instruction, updatedData)
                }
            }
            is WriteValueInstruction -> processAssignmentToVariable(instruction, updatedData)
            is CallInstruction -> {
                when {
                    instruction.element is JetBinaryExpression -> // todo: make this check stronger, check args types
                        processBinaryOperation(instruction.element.operationToken, instruction, updatedData)
                    instruction.element is JetPrefixExpression -> // todo: make this check stronger, check args types
                        processUnaryOperation(instruction.element.operationToken, instruction, updatedData)
                    instruction.element is JetPostfixExpression -> // todo: make this check stronger, check args types
                        processUnaryOperation(instruction.element.operationToken, instruction, updatedData)
                    else -> {
                        val pseudoAnnotation = CallInstructionUtils.tryExtractPseudoAnnotationForCollector(instruction)
                        when (pseudoAnnotation) {
                            null -> Unit
                            is ConstructorWithSizeAsArg,
                            is ConstructorWithElementsAsArgs ->
                                processCollectionCreation(pseudoAnnotation, instruction, updatedData)
                            is SizeMethod ->
                                processSizeMethodCallOnCollection(instruction, updatedData)
                            is IncrSizeByConstantNumberMethod -> {
                                val numberOfElementsToAdd = IntegerVariableValues.Defined(pseudoAnnotation.increaseBy)
                                processIncreaseSizeMethodCallOnCollection(numberOfElementsToAdd, instruction, updatedData)
                            }
                            is IncrSizeByPassedCollectionSizeMethod -> {
                                val numberOfElementsToAdd = tryExtractPassedCollectionSizes(pseudoAnnotation, instruction, updatedData)
                                processIncreaseSizeMethodCallOnCollection(numberOfElementsToAdd, instruction, updatedData)
                            }
                            is DecrSizeToZeroMethod ->
                                processDecreaseSizeMethodCallOnCollection(pseudoAnnotation, instruction, updatedData)
                            else -> Unit
                        }
                    }
                }
            }
            is MagicInstruction -> {
                when(instruction.kind) {
                    MagicKind.LOOP_RANGE_ITERATION -> {
                        // process range operator result storing in fake variable
                        val rangeValues =
                                if (!instruction.inputValues.isEmpty())
                                    updatedData.intFakeVarsToValues.remove(instruction.inputValues[0])
                                else null
                        rangeValues?.let {
                            val target = instruction.outputValue
                            updatedData.intFakeVarsToValues.put(target, it)
                        }
                    }
                    MagicKind.AND, MagicKind.OR -> {
                        // && and || operations are represented as MagicInstruction for some reason (not as CallInstruction)
                        if (instruction.element is JetBinaryExpression) {
                            processBinaryOperation(instruction.element.operationToken, instruction, updatedData)
                        }
                    }
                    MagicKind.EQUALS_IN_WHEN_CONDITION -> {
                        processBinaryOperation(JetTokens.EQEQ, instruction, updatedData)
                    }
                    else -> Unit
                }
            }
        }
        return updatedData
    }

    private fun processVariableDeclaration(instruction: Instruction, updatedData: ValuesData) {
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: return
        val variableType = variableDescriptor.type
        when {
            KotlinBuiltIns.isInt(variableType) ->
                updatedData.intVarsToValues.put(variableDescriptor, IntegerVariableValues.Uninitialized)
            KotlinBuiltIns.isBoolean(variableType) ->
                updatedData.boolVarsToValues.put(variableDescriptor, BooleanVariableValue.undefinedWithNoRestrictions)
            KotlinArrayUtils.isGenericOrPrimitiveArray(variableType),
            KotlinListUtils.isKotlinList(variableType) ->
                updatedData.collectionsToSizes.put(variableDescriptor, IntegerVariableValues.Uninitialized)
        }
    }

    private fun processLiteral(element: JetConstantExpression, instruction: ReadValueInstruction, updatedData: ValuesData) {
        // process literal occurrence (all literals are stored to fake variables by read instruction)
        val node = element.node
        val nodeType = node.elementType as? JetNodeType
                       ?: return
        val fakeVariable = instruction.outputValue
        val valueAsText = node.text
        when (nodeType) {
            JetNodeTypes.INTEGER_CONSTANT ->
                try {
                    val literalValue = valueAsText.toInt()
                    updatedData.intFakeVarsToValues[fakeVariable] = IntegerVariableValues.Defined(literalValue)
                } catch (e: NumberFormatException) { /* not an int literal, so we don't need to do anything with it */ }
            JetNodeTypes.BOOLEAN_CONSTANT -> {
                val booleanValue = valueAsText.toBoolean()
                updatedData.boolFakeVarsToValues.put(fakeVariable, BooleanVariableValue.create(booleanValue))
            }
        }
    }

    private fun processVariableReference(instruction: ReadValueInstruction, updatedData: ValuesData) {
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: return
        val newFakeVariable = instruction.outputValue
        val referencedVariableValue = updatedData.intVarsToValues.getOrElse(
                variableDescriptor, { updatedData.boolVarsToValues.getOrElse(
                variableDescriptor, { updatedData.collectionsToSizes[variableDescriptor] }) })
        when (referencedVariableValue) {
            is IntegerVariableValues ->
                // we have the information about value, so it is definitely of integer type
                // (assuming there are no undeclared variables)
                updatedData.intFakeVarsToValues.put(newFakeVariable, referencedVariableValue.copy())
            is BooleanVariableValue ->
                // we have the information about value, so it is definitely of boolean type
                // (assuming there are no undeclared variables)
                updatedData.boolFakeVarsToValues.put(newFakeVariable, referencedVariableValue.copy())
        }
    }

    private fun processAssignmentToVariable(instruction: WriteValueInstruction, updatedData: ValuesData) {
        // process assignment to variable
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: return
        val fakeVariable = instruction.rValue
        val targetType = tryGetTargetDescriptor(instruction.target)?.returnType
                         ?: return
        when {
            KotlinBuiltIns.isInt(targetType) -> {
                val valuesToAssign = updatedData.intFakeVarsToValues.remove(fakeVariable)?.let { it.copy() }
                                     ?: IntegerVariableValues.Undefined
                updateIntegerValueIfNeeded(instruction, updatedData.intVarsToValues, variableDescriptor, valuesToAssign)
            }
            KotlinBuiltIns.isBoolean(targetType) -> {

                val valueToAssign = updatedData.boolFakeVarsToValues.remove(fakeVariable)?.let { it.copy() }
                                    ?: BooleanVariableValue.undefinedWithNoRestrictions
                updateBooleanVariableIfNeeded(instruction, updatedData.boolVarsToValues, variableDescriptor, valueToAssign)
            }
            KotlinArrayUtils.isGenericOrPrimitiveArray(targetType),
            KotlinListUtils.isKotlinList(targetType) -> {
                val valuesToAssign = updatedData.intFakeVarsToValues.remove(fakeVariable)?.let { it.copy() }
                                     ?: IntegerVariableValues.Undefined
                updateIntegerValueIfNeeded(instruction, updatedData.collectionsToSizes, variableDescriptor, valuesToAssign)
            }
        }
    }

    private fun tryGetTargetDescriptor(target: AccessTarget): CallableDescriptor? {
        return when(target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> target.resolvedCall.resultingDescriptor
            else -> null
        }
    }

    private fun processBinaryOperation(token: IElementType, instruction: OperationInstruction, updatedData: ValuesData) {
        if(instruction.inputValues.size() < 2) {
            // If the code under processing contains error (for example val a = x + 1, where variable x is undefined)
            // the binary operation may have less than 2 arguments
            return;
        }
        val leftOperandVariable = instruction.inputValues[0]
        val rightOperandVariable = instruction.inputValues[1]
        val resultVariable = instruction.outputValue
                             ?: return
        fun performOperation<Op, R>(
                operandsMap: MutableMap<PseudoValue, Op>,
                resultMap: MutableMap<PseudoValue, R>,
                valueToUseIfNoOperands: R,
                operation: (Op, Op) -> R
        ) {
            val leftOperandValues = operandsMap.remove(leftOperandVariable)
            val rightOperandValues = operandsMap.remove(rightOperandVariable)
            if (leftOperandValues != null && rightOperandValues != null) {
                resultMap[resultVariable] = operation(leftOperandValues, rightOperandValues)
            }
            else {
                resultMap[resultVariable] = valueToUseIfNoOperands
            }
        }
        fun intIntOperation(operation: (IntegerVariableValues, IntegerVariableValues) -> IntegerVariableValues) =
            performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues,
                             IntegerVariableValues.Undefined, operation)
        fun intBoolOperation(operation: (IntegerVariableValues, IntegerVariableValues) -> BooleanVariableValue) =
                performOperation(updatedData.intFakeVarsToValues, updatedData.boolFakeVarsToValues,
                                 BooleanVariableValue.undefinedWithNoRestrictions, operation)
        fun boolBoolOperation(operation: (BooleanVariableValue, BooleanVariableValue) -> BooleanVariableValue) =
                performOperation(updatedData.boolFakeVarsToValues, updatedData.boolFakeVarsToValues,
                                 BooleanVariableValue.undefinedWithNoRestrictions, operation)
        val leftOperandDescriptor =
                leftOperandVariable.createdAt?.let { PseudocodeUtil.extractVariableDescriptorIfAny(it, false, bindingContext) }
        when (token) {
            JetTokens.PLUS -> intIntOperation { x, y -> x + y }
            JetTokens.MINUS -> intIntOperation { x, y -> x - y }
            JetTokens.MUL -> intIntOperation { x, y -> x * y }
            JetTokens.DIV -> intIntOperation { x, y -> x / y }
            JetTokens.RANGE -> intIntOperation { x, y -> x .. y }
            JetTokens.EQEQ -> intBoolOperation { x, y -> x.eq(y, leftOperandDescriptor, updatedData) }
            JetTokens.EXCLEQ -> intBoolOperation { x, y -> x.notEq(y, leftOperandDescriptor, updatedData) }
            JetTokens.LT -> intBoolOperation { x, y ->  x.lessThan(y, leftOperandDescriptor, updatedData) }
            JetTokens.GT -> intBoolOperation { x, y -> x.greaterThan(y, leftOperandDescriptor, updatedData) }
            JetTokens.LTEQ -> intBoolOperation { x, y -> x.lessOrEq(y, leftOperandDescriptor, updatedData) }
            JetTokens.GTEQ -> intBoolOperation { x, y -> x.greaterOrEq(y, leftOperandDescriptor, updatedData) }
            JetTokens.OROR -> boolBoolOperation { x, y -> x.or(y) }
            JetTokens.ANDAND -> boolBoolOperation { x, y -> x.and(y) }
        }
    }

    private fun processUnaryOperation(operationToken: IElementType, instruction: CallInstruction, updatedData: ValuesData) {
        assert(instruction.inputValues.size() > 0, "Prefix operation is expected to have at least one input value")
        val operandVariable = instruction.inputValues[0]
        val resultVariable = instruction.outputValue
                             ?: return
        fun performOperation<V>(fakeVariablesMap: MutableMap<PseudoValue, V>, valueToUseIfNoOperands: V, operation: (V) -> V) {
            val operandValues = fakeVariablesMap.remove(operandVariable)
            fakeVariablesMap[resultVariable] = operandValues?.let { operation(it) } ?: valueToUseIfNoOperands
        }
        when (operationToken) {
            JetTokens.MINUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) { -it }
            JetTokens.PLUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) { it.copy() }
            JetTokens.EXCL -> performOperation(updatedData.boolFakeVarsToValues, BooleanVariableValue.undefinedWithNoRestrictions) { !it }
            JetTokens.PLUSPLUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) {
                it + IntegerVariableValues.Defined(1)
            }
            JetTokens.MINUSMINUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) {
                it - IntegerVariableValues.Defined(1)
            }
        }
    }

    private fun processCollectionCreation(pseudoAnnotation: PseudoAnnotation, instruction: CallInstruction, updatedData: ValuesData) {
        val collectionSize = tryExtractCollectionSize(pseudoAnnotation, instruction, updatedData)
        val collectionSizeVariable = instruction.outputValue
        if (collectionSizeVariable != null && collectionSize != null) {
            updatedData.intFakeVarsToValues[collectionSizeVariable] = collectionSize
        }
    }

    private fun tryExtractCollectionSize(
            pseudoAnnotation: PseudoAnnotation,
            instruction: CallInstruction,
            valuesData: ValuesData
    ): IntegerVariableValues? =
            when (pseudoAnnotation) {
                is ConstructorWithElementsAsArgs ->
                    IntegerVariableValues.Defined(instruction.arguments.size())
                is ConstructorWithSizeAsArg -> {
                    if (instruction.inputValues.size() > pseudoAnnotation.sizeArgPosition && pseudoAnnotation.sizeArgPosition >= 0) {
                        valuesData.intFakeVarsToValues.remove(instruction.inputValues[pseudoAnnotation.sizeArgPosition])
                    }
                    else {
                        // Code possibly contains error (like Array<Int>())
                        // so we can't define size
                        null
                    }
                }
                else -> null
            }

    private fun processSizeMethodCallOnCollection(instruction: CallInstruction, updatedData: ValuesData) {
        if (!instruction.inputValues.isEmpty()) {
            val collectionSize = updatedData.intFakeVarsToValues.remove(instruction.inputValues[0])
            val resultVariable = instruction.outputValue
            if (collectionSize != null && resultVariable != null) {
                updatedData.intFakeVarsToValues[resultVariable] = collectionSize
            }
        }
    }

    private fun processIncreaseSizeMethodCallOnCollection(
            numberOfElementsToAdd: IntegerVariableValues?,
            instruction: CallInstruction,
            updatedData: ValuesData
    ) {
        if (instruction.inputValues.size() > 0) {
            val collectionVariableValueSourceInstruction = instruction.inputValues[0].createdAt
                                                           ?: return
            val collectionVariableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                    collectionVariableValueSourceInstruction, false, bindingContext
            ) ?: return
            val collectionSizes = updatedData.collectionsToSizes[collectionVariableDescriptor]
                                  ?: return
            val updateValue = numberOfElementsToAdd?.let { collectionSizes + it } ?: IntegerVariableValues.Undefined
            updateIntegerValueIfNeeded(instruction, updatedData.collectionsToSizes, collectionVariableDescriptor, updateValue)
        }
    }

    private fun tryExtractPassedCollectionSizes(
            pseudoAnnotation: IncrSizeByPassedCollectionSizeMethod,
            instruction: CallInstruction,
            updatedData: ValuesData
    ): IntegerVariableValues? {
        // This function is used to handle the code like `lst.addAll(lst2)`, when we know both `lst` and `lst2` sizes
        fun tryExtractFromKnownCollections(passedCollectionValueSourceInstruction: InstructionWithValue): IntegerVariableValues? {
            val passedCollectionVariableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                    passedCollectionValueSourceInstruction, false, bindingContext
            ) ?: return null
            return updatedData.collectionsToSizes[passedCollectionVariableDescriptor]
        }
        // This function is used to handle the code like `lst.addAll(arrayOf(...))`
        // so we check if the passed value is collection creation
        fun tryExtractFromInstruction(passedCollectionValueSourceInstruction: InstructionWithValue): IntegerVariableValues? {
            if (passedCollectionValueSourceInstruction is CallInstruction) {
                val pseudoAnnotationForCall = CallInstructionUtils.tryExtractPseudoAnnotationForCollector(passedCollectionValueSourceInstruction)
                                              ?: return null
                return tryExtractCollectionSize(pseudoAnnotationForCall, passedCollectionValueSourceInstruction, updatedData)
            }
            return null
        }
        // "+ 1" below is used because the first element of inputValue list is dispatch receiver
        val passedCollectionPosition = pseudoAnnotation.collectionArgPosition + 1
        if (instruction.inputValues.size() > passedCollectionPosition && passedCollectionPosition >= 0) {
            val passedCollectionValueSourceInstruction = instruction.inputValues[passedCollectionPosition].createdAt
                                                         ?: return null
            val sizeFromKnownCollections = tryExtractFromKnownCollections(passedCollectionValueSourceInstruction)
            if (sizeFromKnownCollections != null) {
                return sizeFromKnownCollections
            }
            return tryExtractFromInstruction(passedCollectionValueSourceInstruction)
        }
        return null
    }

    private fun processDecreaseSizeMethodCallOnCollection(
            pseudoAnnotation: PseudoAnnotation,
            instruction: CallInstruction,
            updatedData: ValuesData
    ) {
        if (instruction.inputValues.size() > 0) {
            val collectionVariableValueSourceInstruction = instruction.inputValues[0].createdAt
                                                           ?: return
            val collectionVariableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                    collectionVariableValueSourceInstruction, false, bindingContext
            ) ?: return
            if (updatedData.collectionsToSizes[collectionVariableDescriptor] is IntegerVariableValues.Dead) {
                return
            }
            when(pseudoAnnotation) {
                is DecrSizeToZeroMethod ->
                    updatedData.collectionsToSizes[collectionVariableDescriptor] = IntegerVariableValues.Defined(0)
            }
        }
    }

    private fun updateIntegerValueIfNeeded(
            instruction: Instruction,
            collection: MutableMap<VariableDescriptor, IntegerVariableValues>,
            variableDescriptor: VariableDescriptor,
            updateValue: IntegerVariableValues
    ) {
        val targetVariableValues = collection[variableDescriptor]
        if (targetVariableValues is IntegerVariableValues.Dead) {
            return
        }
        val variableLexicalScopeDepth = lexicalScopeVariableInfo.declaredIn[variableDescriptor]?.depth
                                        ?: return
        val currentLexicalScopeDepth = instruction.lexicalScope.depth
        if (loopsInfoMap[instruction] == true && variableLexicalScopeDepth < currentLexicalScopeDepth) {
            // external variable modification inside loop is not supported
            when (targetVariableValues) {
                is IntegerVariableValues.Uninitialized ->
                    collection[variableDescriptor] = IntegerVariableValues.Undefined
                is IntegerVariableValues.Defined ->
                    targetVariableValues.setNotAllPossibleValuesKnown()
                else -> Unit
            }
        }
        else {
            collection[variableDescriptor] = updateValue
        }
    }

    private fun updateBooleanVariableIfNeeded(
            instruction: Instruction,
            collection: MutableMap<VariableDescriptor, BooleanVariableValue>,
            variableDescriptor: VariableDescriptor,
            updateValue: BooleanVariableValue
    ) {
        val variableLexicalScopeDepth = lexicalScopeVariableInfo.declaredIn[variableDescriptor]?.depth
                                        ?: return
        val currentLexicalScopeDepth = instruction.lexicalScope.depth
        if (loopsInfoMap[instruction] == true && variableLexicalScopeDepth < currentLexicalScopeDepth) {
            // external variable modification inside loop is not supported
            val targetVariableValues = collection[variableDescriptor]
            when (targetVariableValues) {
                is BooleanVariableValue.Undefined -> Unit
                else -> collection[variableDescriptor] = BooleanVariableValue.undefinedWithNoRestrictions
            }
        }
        else {
            collection[variableDescriptor] = updateValue
        }
    }
}