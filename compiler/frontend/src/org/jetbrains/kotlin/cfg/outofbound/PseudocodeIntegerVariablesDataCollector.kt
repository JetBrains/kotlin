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
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.MarkInstruction
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

// This file contains functionality similar to org.jetbrains.kotlin.cfg.PseudocodeVariablesData,
// but collects information about integer variables' values. Semantically it would be better to
// merge functionality in this two files

public class PseudocodeIntegerVariablesDataCollector(val pseudocode: Pseudocode, val bindingContext: BindingContext) {
    private val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)
    // The map below is used to define indices of instructions. To achieve O(1) operation time cost
    // the map is created with capacity = instructions number (+ constant) and load factor = 1
    private val instructionsToTheirIndices: HashMap<Instruction, Int> = createInstructionsToTheirIndicesMap()

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

    // todo: improve implementation reducing traverses from 2 to 1
    private fun createInstructionsToTheirIndicesMap(): HashMap<Instruction, Int> {
        var instructionsNumber = 0
        pseudocode.traverse(TraversalOrder.FORWARD, { i -> ++instructionsNumber })
        val resultingMap = HashMap<Instruction, Int>(instructionsNumber + 5, 1f) // + 5 is chosen randomly
        var index = 0
        pseudocode.traverse(TraversalOrder.FORWARD, { instruction -> resultingMap[instruction] = index; ++index })
        return resultingMap
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
        assert(instructionsToTheirIndices.containsKey(previousInstruction), "Mapping from instructions to their indices is wrong")
        assert(instructionsToTheirIndices.containsKey(currentInstruction), "Mapping from instructions to their indices is wrong")
        val prevInstructionIndex = instructionsToTheirIndices[previousInstruction] as Int
        val curInstructionIndex = instructionsToTheirIndices[currentInstruction] as Int
        if (prevInstructionIndex > curInstructionIndex) {
            // The edge we need to update leads from loop end to loop enter (for example, from while loop's body end
            // to while loop's condition). After the first traversal of all the instructions list, this edge will contain
            // the information that is computed after current instruction. In current implementation we don't process loop
            // bodies multiple times, so to avoid this computation we destroy the information on this edge.
            return ValuesData()
        }
        val updatedEdgeData = edgeData.copy()
        val filteredEdgeData = removeOutOfScopeVariables(previousInstruction, currentInstruction, updatedEdgeData)
        removeUnavailableValuesIfNeeded(previousInstruction, filteredEdgeData)
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

    private fun removeUnavailableValuesIfNeeded(
            previousInstruction: Instruction,
            edgeData: ValuesData
    ) {
        if (previousInstruction is MarkInstruction && previousInstruction.previousInstructions.size() == 1) {
            val beforePreviousInstruction = previousInstruction.previousInstructions.first()
            if (beforePreviousInstruction is ConditionalJumpInstruction && beforePreviousInstruction.element is JetIfExpression) {
                val conditionFakeVariable = beforePreviousInstruction.conditionValue
                val conditionBoolValue = edgeData.boolFakeVarsToValues[conditionFakeVariable]
                if (conditionBoolValue != null) {
                    when (conditionBoolValue) {
                        is BooleanVariableValue.True -> {
                            if (beforePreviousInstruction.nextOnFalse == previousInstruction) {
                                // We are in "else" block and condition evaluated to "true"
                                // so this block will not be processed. For now to indicate this
                                // we will make all variables undefined
                                edgeData.intVarsToValues.forEach { it.value.setUndefined() }
                            }
                        }
                        is BooleanVariableValue.False -> {
                            if (beforePreviousInstruction.nextOnTrue == previousInstruction) {
                                // We are in "then" block and condition evaluated to "false"
                                // so this block will not be processed. For now to indicate this
                                // we will make all variables undefined
                                edgeData.intVarsToValues.forEach { it.value.setUndefined() }
                            }
                        }
                        is BooleanVariableValue.Undefined -> {
                            if (beforePreviousInstruction.nextOnTrue == previousInstruction) {
                                // We are in "then" block and need to apply onTrue restrictions
                                for ((variable, unrestrictedValues) in conditionBoolValue.onTrueRestrictions) {
                                    edgeData.intVarsToValues[variable]?.leaveOnlyValuesInSet(unrestrictedValues)
                                }
                            }
                            else {
                                assert(beforePreviousInstruction.nextOnFalse == previousInstruction)
                                // We are in "else" block and need to apply onFalse restrictions
                                for ((variable, unrestrictedValues) in conditionBoolValue.onFalseRestrictions) {
                                    edgeData.intVarsToValues[variable]?.leaveOnlyValuesInSet(unrestrictedValues)
                                }
                            }
                        }
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
            unitedIntFakeVariables.putAll(data.intFakeVarsToValues)
            MapUtils.mergeMapsIntoFirst(unitedBoolVariables, data.boolVarsToValues) { value1, value2 -> value1.or(value2) }
            unitedBoolFakeVariables.putAll(data.boolFakeVarsToValues)
            mergeCorrespondingIntegerVariables(unitedArrayVariables, data.collectionsToSizes)
        }
        return ValuesData(unitedIntVariables, unitedIntFakeVariables, unitedBoolVariables, unitedBoolFakeVariables, unitedArrayVariables)
    }

    private fun mergeCorrespondingIntegerVariables<K>(
            targetVariablesMap: MutableMap<K, IntegerVariableValues>,
            variablesToConsume: MutableMap<K, IntegerVariableValues>
    ) {
        MapUtils.mergeMapsIntoFirst(targetVariablesMap, variablesToConsume) { value1, value2 ->
            if (value1.isDefined) {
                value1.addAll(value2)
                value1
            }
            else value2
        }
    }

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
                    else -> {
                        val callInfo = CallInstructionUtils.tryExtractCallInfo(instruction)
                        if(callInfo != null) {
                            when {
                                CallInstructionUtils.returnTypeIsCollection(callInfo) ->
                                    processCollectionCreation(callInfo, instruction, updatedData)
                                isSizeMethodCallOnCollection(callInfo) ->
                                    processSizeMethodCallOnCollection(instruction, updatedData)
                                isIncreaseSizeMethodCallOnCollection(callInfo) ->
                                    processIncreaseSizeMethodCallOnCollection(callInfo, instruction, updatedData)
                                else -> Unit
                            }
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
                                    updatedData.intFakeVarsToValues[instruction.inputValues[0]]
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
                updatedData.intVarsToValues.put(variableDescriptor, IntegerVariableValues())
            KotlinBuiltIns.isBoolean(variableType) ->
                updatedData.boolVarsToValues.put(variableDescriptor, BooleanVariableValue.undefinedWithNoRestrictions)
            KotlinArrayUtils.isGenericOrPrimitiveArray(variableType),
            KotlinListUtils.isKotlinList(variableType) ->
                updatedData.collectionsToSizes.put(variableDescriptor, IntegerVariableValues())
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
                    updatedData.intFakeVarsToValues[fakeVariable] = IntegerVariableValues(literalValue)
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
                val valuesToAssign = updatedData.intFakeVarsToValues[fakeVariable]
                updatedData.intVarsToValues.put(
                        variableDescriptor,
                        valuesToAssign?.let { it.copy() } ?: IntegerVariableValues.createUndefined()
                )
            }
            KotlinBuiltIns.isBoolean(targetType) -> {
                val valueToAssign = updatedData.boolFakeVarsToValues[fakeVariable]
                updatedData.boolVarsToValues.put(
                        variableDescriptor,
                        valueToAssign?.let { it.copy() } ?: BooleanVariableValue.undefinedWithNoRestrictions
                )
            }
            KotlinArrayUtils.isGenericOrPrimitiveArray(targetType),
            KotlinListUtils.isKotlinList(targetType) -> {
                val valuesToAssign = updatedData.intFakeVarsToValues[fakeVariable]
                updatedData.collectionsToSizes.put(
                        variableDescriptor,
                        valuesToAssign?.let { it.copy() } ?: IntegerVariableValues.createUndefined()
                )
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
            val leftOperandValues = operandsMap[leftOperandVariable]
            val rightOperandValues = operandsMap[rightOperandVariable]
            if (leftOperandValues != null && rightOperandValues != null) {
                resultMap[resultVariable] = operation(leftOperandValues, rightOperandValues)
            }
            else {
                resultMap[resultVariable] = valueToUseIfNoOperands
            }
        }
        fun intIntOperation(operation: (IntegerVariableValues, IntegerVariableValues) -> IntegerVariableValues) =
            performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues,
                             IntegerVariableValues.createUndefined(), operation)
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
            val operandValues = fakeVariablesMap[operandVariable]
            fakeVariablesMap[resultVariable] = operandValues?.let { operation(it) } ?: valueToUseIfNoOperands
        }
        when (operationToken) {
            JetTokens.MINUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.createUndefined()) { -it }
            JetTokens.PLUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.createUndefined()) { it.copy() }
            JetTokens.EXCL -> performOperation(updatedData.boolFakeVarsToValues, BooleanVariableValue.undefinedWithNoRestrictions) { !it }
        }
    }

    private fun processCollectionCreation(callInfo: CallInstructionUtils.CallInfo, instruction: CallInstruction, updatedData: ValuesData) {
        val collectionSize = tryExtractCollectionSize(callInfo, instruction, updatedData)
        val collectionSizeVariable = instruction.outputValue
        if (collectionSizeVariable != null && collectionSize != null) {
            updatedData.intFakeVarsToValues[collectionSizeVariable] = collectionSize
        }
    }

    private fun tryExtractCollectionSize(
            callInfo: CallInstructionUtils.CallInfo,
            instruction: CallInstruction,
            valuesData: ValuesData
    ): IntegerVariableValues? =
            when (callInfo.calledName) {
                KotlinArrayUtils.arrayOfFunctionName,
                KotlinListUtils.listOfFunctionName,
                KotlinListUtils.arrayListOfFunctionName ->
                    IntegerVariableValues(instruction.arguments.size())
                KotlinArrayUtils.arrayConstructorName,
                in KotlinArrayUtils.primitiveArrayConstructorNames -> {
                    if (instruction.inputValues.isEmpty()) {
                        // Code possibly contains error (like Array<Int>())
                        // so we can't define size
                        null
                    }
                    else {
                        valuesData.intFakeVarsToValues[instruction.inputValues[0]]
                    }
                }
                else -> null
            }

    private fun isSizeMethodCallOnCollection(callInfo: CallInstructionUtils.CallInfo): Boolean =
        CallInstructionUtils.checkMethodCallOnCollection(
                callInfo,
                { it == KotlinCollectionsUtils.sizeMethodName },
                { KotlinBuiltIns.isInt(it) }
        )

    private fun processSizeMethodCallOnCollection(instruction: CallInstruction, updatedData: ValuesData) {
        if (!instruction.inputValues.isEmpty()) {
            val collectionSize = updatedData.intFakeVarsToValues[instruction.inputValues[0]]
            val resultVariable = instruction.outputValue
            if (collectionSize != null && resultVariable != null) {
                updatedData.intFakeVarsToValues[resultVariable] = collectionSize
            }
        }
    }

    private fun isIncreaseSizeMethodCallOnCollection(callInfo: CallInstructionUtils.CallInfo): Boolean =
            CallInstructionUtils.checkMethodCallOnCollection(
                    callInfo,
                    { it == KotlinListUtils.addMethodName },
                    { KotlinBuiltIns.isBoolean(it) || KotlinBuiltIns.isUnit(it) }
            )
            ||
            CallInstructionUtils.checkMethodCallOnCollection(
                    callInfo,
                    { it == KotlinListUtils.addAllMethodName },
                    { KotlinBuiltIns.isBoolean(it) || KotlinBuiltIns.isUnit(it) }
            )

    private fun processIncreaseSizeMethodCallOnCollection(callInfo: CallInstructionUtils.CallInfo, instruction: CallInstruction, updatedData: ValuesData) {
        if (instruction.inputValues.size() > 0) {
            val collectionVariableValueSourceInstruction = instruction.inputValues[0].createdAt
                                                           ?: return
            val collectionVariableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                    collectionVariableValueSourceInstruction, false, bindingContext
            ) ?: return
            val collectionSizes = updatedData.collectionsToSizes[collectionVariableDescriptor]
                                  ?: return
            val numberOfElementsToAdd: IntegerVariableValues? = when(callInfo.calledName) {
                KotlinListUtils.addMethodName -> IntegerVariableValues(1)
                KotlinListUtils.addAllMethodName -> tryExtractPassedCollectionSizes(instruction, updatedData)
                else -> null
            }
            updatedData.collectionsToSizes[collectionVariableDescriptor] =
                    numberOfElementsToAdd?.let { collectionSizes + it }
                    ?: IntegerVariableValues.createUndefined()
        }
    }

    private fun tryExtractPassedCollectionSizes(instruction: CallInstruction, updatedData: ValuesData): IntegerVariableValues? {
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
                val callInfo = CallInstructionUtils.tryExtractCallInfo(passedCollectionValueSourceInstruction)
                               ?: return null
                if (CallInstructionUtils.returnTypeIsCollection(callInfo)) {
                    return tryExtractCollectionSize(callInfo, passedCollectionValueSourceInstruction, updatedData)
                }
                return null
            }
            return null
        }
        if (instruction.inputValues.size() > 1) {
            val passedCollectionValueSourceInstruction = instruction.inputValues[1].createdAt
                                                         ?: return null
            val sizeFromKnownCollections = tryExtractFromKnownCollections(passedCollectionValueSourceInstruction)
            if (sizeFromKnownCollections != null) {
                return sizeFromKnownCollections
            }
            return tryExtractFromInstruction(passedCollectionValueSourceInstruction)
        }
        return null
    }
}