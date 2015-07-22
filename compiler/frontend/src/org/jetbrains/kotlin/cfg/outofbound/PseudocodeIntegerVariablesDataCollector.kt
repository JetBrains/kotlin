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
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.keysToMapExceptNulls
import java.lang.Boolean
import java.util.*
import kotlin.properties.Delegates

// This file contains functionality similar to org.jetbrains.kotlin.cfg.PseudocodeVariablesData,
// but collects information about integer variables' values. Semantically it would be better to
// merge functionality in this two files

public class PseudocodeIntegerVariablesDataCollector(val pseudocode: Pseudocode, val bindingContext: BindingContext) {
    private val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)
    public val integerVariablesValues: Map<Instruction, Edges<ValuesData>> = collectVariableValuesData()

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

    private  fun collectVariableValuesData(): Map<Instruction, Edges<ValuesData>> {
        return pseudocode.collectData(
                TraversalOrder.FORWARD,
                /* mergeDataWithLocalDeclarations */ true,
                { instruction, incomingData: Collection<ValuesData> -> mergeVariablesValues(instruction, incomingData) },
                { previous, current, edgeData -> updateEdge(previous, current, edgeData) },
                ValuesData.createEmpty()
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
        return ValuesData(HashMap(filteredIntVars), HashMap(filteredIntFakeVars), HashMap(filteredBoolVars), HashMap(filteredBoolFakeVars))
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
        if(previousInstruction is MarkInstruction && previousInstruction.previousInstructions.size() == 1) {
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
                                    edgeData.intVarsToValues.get(variable)
                                            ?.leaveOnlyPassedValuesAvailable(unrestrictedValues, currentInstruction.lexicalScope)
                                }
                            }
                            else {
                                // We are in "else" block and need to apply onFalse restrictions
                                for ((variable, unrestrictedValues) in conditionBoolValue.onFalseRestrictions) {
                                    edgeData.intVarsToValues.get(variable)
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
        if(previousInstruction.lexicalScope.depth > currentInstruction.lexicalScope.depth) {
            edgeData.intVarsToValues.values().forEach { it.tryMakeValuesAvailable(currentInstruction.lexicalScope) }
            edgeData.intFakeVarsToValues.values().forEach { it.tryMakeValuesAvailable(currentInstruction.lexicalScope) }
        }
    }

    private fun mergeVariablesValues(instruction: Instruction, incomingEdgesData: Collection<ValuesData>): Edges<ValuesData> {
        if(instruction is SubroutineSinkInstruction) {
            // this instruction is assumed to be the last one in function so it is not processed
            return Edges(ValuesData.createEmpty(), ValuesData.createEmpty())
        }
        val enterInstructionData = unionIncomingVariablesValues(incomingEdgesData)
        val exitInstructionData = updateValues(instruction, enterInstructionData)
        return Edges(enterInstructionData, exitInstructionData)
    }

    private fun unionIncomingVariablesValues(incomingEdgesData: Collection<ValuesData>): ValuesData {
        if(incomingEdgesData.isEmpty()) {
            return ValuesData.createEmpty()
        }
        val headData = incomingEdgesData.first()
        val tailData = incomingEdgesData.drop(1)
        val unitedIntVariables = headData.intVarsToValues
        val unitedIntFakeVariables = headData.intFakeVarsToValues
        val unitedBoolVariables = headData.boolVarsToValues
        val unitedBoolFakeVariables = headData.boolFakeVarsToValues
        for (data in tailData) {
            mergeCorrespondingVariables(unitedIntVariables, data.intVarsToValues)
            unitedIntFakeVariables.putAll(data.intFakeVarsToValues)
            unitedBoolVariables.putAll(data.boolVarsToValues) // TODO: this is stub, makes no sense
            unitedBoolFakeVariables.putAll(data.boolFakeVarsToValues)
        }
        return ValuesData(unitedIntVariables, unitedIntFakeVariables, unitedBoolVariables, unitedBoolFakeVariables)
    }

    private fun mergeCorrespondingVariables<K>(
            targetVariablesMap: MutableMap<K, IntegerVariableValues>,
            variablesToConsume: MutableMap<K, IntegerVariableValues>
    ) {
        val targetMapKeys = HashSet(targetVariablesMap.keySet())
        for(key in targetMapKeys) {
            val values1 = targetVariablesMap.get(key) ?: throw Exception("No corresponding element in map")
            val values2 = variablesToConsume.get(key) ?: throw Exception("No corresponding element in map")
            if(values1.cantBeDefined || values2.cantBeDefined) {
                values1.setCantBeDefined()
            } else {
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
                if(instruction.element is JetBinaryExpression) {
                    processBinaryOperation(instruction.element.getOperationToken(), instruction, updatedData)
                }
            }
            is MagicInstruction -> {
                if(instruction.kind == MagicKind.LOOP_RANGE_ITERATION) {
                    // process range operator result storing in fake variable
                    assert(instruction.inputValues.size() == 1, "Loop range iteration is assumed to have 1 input value")
                    val rangeValuesFakeVariable = instruction.inputValues.get(0)
                    val rangeValues = updatedData.intFakeVarsToValues.get(rangeValuesFakeVariable)
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
                                 ?: throw Exception("Variable descriptor is null")
        val variableType = variableDescriptor.getType()
        when {
            KotlinBuiltIns.isInt(variableType) ->
                updatedData.intVarsToValues.put(variableDescriptor, IntegerVariableValues.createEmpty())
            KotlinBuiltIns.isBoolean(variableType) ->
                updatedData.boolVarsToValues.put(variableDescriptor, BooleanVariableValue.undefinedWithNoRestrictions)
        }
    }

    private fun processLiteral(element: JetConstantExpression, instruction: ReadValueInstruction, updatedData: ValuesData) {
        // process literal occurrence (all literals are stored to fake variables by read instruction)
        val node = element.getNode()
        val nodeType = node.getElementType() as? JetNodeType
                       ?: throw Exception("Node's elementType has wrong type for JetConstantExpression")
        val fakeVariable = instruction.outputValue
        val valueAsText = node.getText()
        when (nodeType) {
            JetNodeTypes.INTEGER_CONSTANT -> {
                val literalValue = Integer.parseInt(valueAsText)
                updatedData.intFakeVarsToValues.put(fakeVariable, IntegerVariableValues.createSingleton(literalValue))
            }
            JetNodeTypes.BOOLEAN_CONSTANT -> {
                val booleanValue = Boolean.parseBoolean(valueAsText)
                updatedData.boolFakeVarsToValues.put(fakeVariable, BooleanVariableValue.createTrueOrFalse(booleanValue))
            }
        }
    }

    private fun processVariableReference(instruction: ReadValueInstruction, updatedData: ValuesData) {
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: throw Exception("Variable descriptor is null")
        val newFakeVariable = instruction.outputValue
        val referencedVariableValue = updatedData.intVarsToValues.getOrElse(
                variableDescriptor, { updatedData.boolVarsToValues.get(variableDescriptor) })
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
                                 ?: throw Exception("Variable descriptor is null")
        val fakeVariable = instruction.rValue
        val targetType = tryGetTargetDescriptor(instruction.target)?.getReturnType()
                         ?: throw Exception("Cannot define target type in assignment")
        when {
            KotlinBuiltIns.isInt(targetType) -> {
                val valuesToAssign = updatedData.intFakeVarsToValues.get(fakeVariable)
                if (valuesToAssign != null) {
                    updatedData.intVarsToValues.put(variableDescriptor, valuesToAssign.copy())
                } else {
                    updatedData.intVarsToValues.put(variableDescriptor, IntegerVariableValues.createCantBeDefined())
                }
            }
            KotlinBuiltIns.isBoolean(targetType) -> {
                val valueToAssign = updatedData.boolFakeVarsToValues.get(fakeVariable)
                if (valueToAssign != null) {
                    updatedData.boolVarsToValues.put(variableDescriptor, valueToAssign.copy())
                } else {
                    updatedData.boolVarsToValues.put(variableDescriptor, BooleanVariableValue.undefinedWithNoRestrictions)
                }
            }
        }
    }

    private fun processBinaryOperation(token: IElementType, instruction: CallInstruction, updatedData: ValuesData) {
        assert(instruction.inputValues.size().equals(2),
               "Binary expression instruction is supposed to have two input values")
        val leftOperandVariable = instruction.inputValues.get(0)
        val rightOperandVariable = instruction.inputValues.get(1)
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
    
    private fun tryGetTargetDescriptor(target: AccessTarget): CallableDescriptor? {
        return when(target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> target.resolvedCall.getResultingDescriptor()
            else -> null
        }
    }
}