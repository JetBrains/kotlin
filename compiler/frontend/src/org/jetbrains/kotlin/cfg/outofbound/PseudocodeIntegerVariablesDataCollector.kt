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
import com.sun.org.apache.xml.internal.security.keys.content.KeyValue
import org.jetbrains.kotlin.JetNodeType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg
import org.jetbrains.kotlin.cfg.LexicalScopeVariableInfo
import org.jetbrains.kotlin.cfg.LexicalScopeVariableInfoImpl
import org.jetbrains.kotlin.cfg.outofbound.BooleanVariableValue
import org.jetbrains.kotlin.cfg.outofbound.IntegerVariableValues
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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.keysToMapExceptNulls
import java.lang.Boolean as JavaBoolean
import java.util.*
import kotlin.properties.Delegates

// This file contains functionality similar to org.jetbrains.kotlin.cfg.PseudocodeVariablesData,
// but collects information about integer variables' values. Semantically it would be better to
// merge functionality in this two files

public class PseudocodeIntegerVariablesDataCollector(val pseudocode: Pseudocode, val bindingContext: BindingContext) {
    private val arrayOfFunctionName: String = "arrayOf"
    private val arrayConstructorName: String = "Array"
    private val intArrayConstructorName = "IntArray"

    private val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)

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
        makeVariablesUnavailableIfNeeded(previousInstruction, currentInstruction, filteredEdgeData)
        makeVariablesAvailableIfNeeded(previousInstruction, currentInstruction, filteredEdgeData)
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
        val filteredArrayVars = filterOutVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.arraysToSizes)
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

    private fun makeVariablesUnavailableIfNeeded(
            previousInstruction: Instruction,
            currentInstruction: Instruction,
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
                                // we will make all variables completely unavailable
                                edgeData.intVarsToValues.forEach { it.value.makeAllValuesUnavailable(currentInstruction.lexicalScope) }
                            }
                        }
                        is BooleanVariableValue.False -> {
                            if (beforePreviousInstruction.nextOnTrue == previousInstruction) {
                                // We are in "then" block and condition evaluated to "false"
                                // so this block will not be processed. For now to indicate this
                                // we will make all variables completely unavailable
                                edgeData.intVarsToValues.forEach { it.value.makeAllValuesUnavailable(currentInstruction.lexicalScope) }
                            }
                        }
                        is BooleanVariableValue.Undefined -> {
                            if (beforePreviousInstruction.nextOnTrue == previousInstruction) {
                                // We are in "then" block and need to apply onTrue restrictions
                                for ((variable, unrestrictedValues) in conditionBoolValue.onTrueRestrictions) {
                                    edgeData.intVarsToValues[variable]
                                            ?.leaveOnlyPassedValuesAvailable(unrestrictedValues, currentInstruction.lexicalScope)
                                }
                            }
                            else {
                                assert(beforePreviousInstruction.nextOnFalse == previousInstruction)
                                // We are in "else" block and need to apply onFalse restrictions
                                for ((variable, unrestrictedValues) in conditionBoolValue.onFalseRestrictions) {
                                    edgeData.intVarsToValues[variable]
                                            ?.leaveOnlyPassedValuesAvailable(unrestrictedValues, currentInstruction.lexicalScope)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun makeVariablesAvailableIfNeeded(
            previousInstruction: Instruction,
            currentInstruction: Instruction,
            edgeData: ValuesData
    ) {
        if (previousInstruction.lexicalScope.depth > currentInstruction.lexicalScope.depth) {
            edgeData.intVarsToValues.values().forEach { it.tryMakeValuesAvailable(currentInstruction.lexicalScope) }
            edgeData.intFakeVarsToValues.values().forEach { it.tryMakeValuesAvailable(currentInstruction.lexicalScope) }
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
        val unitedArrayVariables = headData.arraysToSizes
        for (data in tailData) {
            mergeCorrespondingVariables(unitedIntVariables, data.intVarsToValues)
            unitedIntFakeVariables.putAll(data.intFakeVarsToValues)
            unitedBoolVariables.putAll(data.boolVarsToValues) // TODO: this is stub, makes no sense
            unitedBoolFakeVariables.putAll(data.boolFakeVarsToValues)
            mergeCorrespondingVariables(unitedArrayVariables, data.arraysToSizes)
        }
        return ValuesData(unitedIntVariables, unitedIntFakeVariables, unitedBoolVariables, unitedBoolFakeVariables, unitedArrayVariables)
    }

    private fun mergeCorrespondingVariables<K>(
            targetVariablesMap: MutableMap<K, IntegerVariableValues>,
            variablesToConsume: MutableMap<K, IntegerVariableValues>
    ) {
        val targetMapKeys = HashSet(targetVariablesMap.keySet())
        for(key in targetMapKeys) {
            val values1 = targetVariablesMap[key] ?: throw Exception("No corresponding element in map")
            val values2 = variablesToConsume[key] ?: throw Exception("No corresponding element in map")
            if (values1.isUndefined || values2.isUndefined) {
                values1.setUndefined()
            }
            else {
                values1.addAll(values2)
            }
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
                    isArrayCreation(instruction) -> processArrayCreation(instruction, updatedData)
                    instruction.element is JetBinaryExpression ->
                        processBinaryOperation(instruction.element.getOperationToken(), instruction, updatedData)
                    instruction.element is JetPrefixExpression ->
                        processUnaryOperation(instruction.element.getOperationToken(), instruction, updatedData)
                }
            }
            is MagicInstruction -> {
                if (instruction.kind == MagicKind.LOOP_RANGE_ITERATION) {
                    // process range operator result storing in fake variable
                    assert(instruction.inputValues.size() == 1, "Loop range iteration is assumed to have 1 input value")
                    val rangeValuesFakeVariable = instruction.inputValues[0]
                    val rangeValues = updatedData.intFakeVarsToValues[rangeValuesFakeVariable]
                                      ?: throw Exception("Range values are not computed")
                    val target = instruction.outputValue
                    updatedData.intFakeVarsToValues.put(target, rangeValues)
                }
            }
        }
        return updatedData
    }

    private fun processVariableDeclaration(instruction: Instruction, updatedData: ValuesData) {
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: return
        val variableType = variableDescriptor.getType()
        when {
            KotlinBuiltIns.isInt(variableType) ->
                updatedData.intVarsToValues.put(variableDescriptor, IntegerVariableValues())
            KotlinBuiltIns.isBoolean(variableType) ->
                updatedData.boolVarsToValues.put(variableDescriptor, BooleanVariableValue.undefinedWithNoRestrictions)
        }
    }

    private fun processLiteral(element: JetConstantExpression, instruction: ReadValueInstruction, updatedData: ValuesData) {
        // process literal occurrence (all literals are stored to fake variables by read instruction)
        val node = element.getNode()
        val nodeType = node.getElementType() as? JetNodeType
                       ?: return
        val fakeVariable = instruction.outputValue
        val valueAsText = node.getText()
        when (nodeType) {
            JetNodeTypes.INTEGER_CONSTANT -> {
                val literalValue = Integer.parseInt(valueAsText)
                updatedData.intFakeVarsToValues.put(fakeVariable, IntegerVariableValues(literalValue))
            }
            JetNodeTypes.BOOLEAN_CONSTANT -> {
                val booleanValue = JavaBoolean.parseBoolean(valueAsText)
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
                variableDescriptor, { updatedData.arraysToSizes[variableDescriptor] }) })
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
        val targetType = tryGetTargetDescriptor(instruction.target)?.getReturnType()
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
            KotlinBuiltIns.isArray(targetType) -> {
                val valuesToAssign = updatedData.intFakeVarsToValues[fakeVariable]
                updatedData.arraysToSizes.put(
                        variableDescriptor,
                        valuesToAssign?.let { it.copy() } ?: IntegerVariableValues.createUndefined()
                )
            }
        }
    }

    private fun tryGetTargetDescriptor(target: AccessTarget): CallableDescriptor? {
        return when(target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> target.resolvedCall.getResultingDescriptor()
            else -> null
        }
    }

    private fun isArrayCreation(instruction: CallInstruction): Boolean {
        // todo: change to annotations checking
        return instruction.resolvedCall
                .getCandidateDescriptor()
                .getReturnType()
                ?.let { KotlinBuiltIns.isArray(it) }
               ?: false
    }

    private fun processArrayCreation(instruction: CallInstruction, updatedData: ValuesData) {
        val arraySize = tryExtractArraySize(instruction, updatedData)
        val arraySizeVariable = instruction.outputValue
        if (arraySizeVariable != null && arraySize != null) {
            updatedData.intFakeVarsToValues[arraySizeVariable] = arraySize
        }
    }

    private fun tryExtractArraySize(instruction: CallInstruction, valuesData: ValuesData): IntegerVariableValues? {
        if(instruction.element is JetCallExpression) {
            val calledName = JetExpressionUtils.tryGetCalledName(instruction.element)
            return when(calledName) {
                arrayOfFunctionName -> IntegerVariableValues(instruction.arguments.size())
                arrayConstructorName, intArrayConstructorName -> {
                    // there are other kinds of array constructors (for example, ByteArray)
                    // and they will be added later
                    assert(instruction.inputValues.size() >= 1,
                           "Array creation calls are expected to have at least one argument")
                    valuesData.intFakeVarsToValues[instruction.inputValues[0]]
                }
                else -> null
            }
        }
        return null
    }

    private fun processBinaryOperation(token: IElementType, instruction: CallInstruction, updatedData: ValuesData) {
        assert(instruction.inputValues.size() >= 2,
               "Binary expression instruction is supposed to have two input values")
        val leftOperandVariable = instruction.inputValues[0]
        val rightOperandVariable = instruction.inputValues[1]
        val resultVariable = instruction.outputValue
                             ?: return
        fun performOperation<Op, R>(
                operandsMap: MutableMap<PseudoValue, Op>,
                resultMap: MutableMap<PseudoValue, R>,
                operation: (Op, Op) -> R
        ) {
            val leftOperandValues = operandsMap[leftOperandVariable]
            val rightOperandValues = operandsMap[rightOperandVariable]
            if (leftOperandValues != null && rightOperandValues != null) {
                resultMap.put(resultVariable, operation(leftOperandValues, rightOperandValues))
            }
        }
        val leftOperandDescriptor =
                leftOperandVariable.createdAt?.let { PseudocodeUtil.extractVariableDescriptorIfAny(it, false, bindingContext) }
        when (token) {
            JetTokens.PLUS ->
                performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues) { x, y -> x + y }
            JetTokens.MINUS ->
                performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues) { x, y -> x - y }
            JetTokens.MUL ->
                performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues) { x, y -> x * y }
            JetTokens.DIV ->
                performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues) { x, y -> x / y }
            JetTokens.RANGE ->
                performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues) { x, y -> x .. y }
            JetTokens.LT -> performOperation(updatedData.intFakeVarsToValues, updatedData.boolFakeVarsToValues) { x, y ->
                x.less(y, leftOperandDescriptor, updatedData)
            }
        }
    }

    private fun processUnaryOperation(operationToken: IElementType, instruction: CallInstruction, updatedData: ValuesData) {
        assert(instruction.inputValues.size() > 0, "Prefix operation is expected to have at least one input value")
        val operandVariable = instruction.inputValues[0]
        val operandValues = updatedData.intFakeVarsToValues[operandVariable]
                            ?: return
        val resultVariable = instruction.outputValue
                             ?: return
        when(operationToken) {
            JetTokens.MINUS -> updatedData.intFakeVarsToValues[resultVariable] = -operandValues
            JetTokens.PLUS -> updatedData.intFakeVarsToValues[resultVariable] = operandValues.copy()
        }
    }
}