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

package org.jetbrains.kotlin.cfg

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.JetNodeType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.cfg
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.collectData
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType
import java.util.*
import kotlin.properties.Delegates

// This file contains functionality similar to org.jetbrains.kotlin.cfg.PseudocodeVariablesData,
// but collects information about integer variables' values. Semantically it would be better to
// merge functionality in this two files

public class PseudocodeIntegerVariablesDataCollector(val pseudocode: Pseudocode, val bindingContext: BindingContext) {
    public var integerVariablesValues: Map<Instruction, Edges<ValuesData>> = collectVariableValuesData()
        private set

    public data class ValuesData(
            val variablesToValues: MutableMap<String, IntegerVariableValues>,
            val fakeVariablesToValues: MutableMap<String, IntegerVariableValues>
    )

    public fun recollectIntegerVariablesValues() {
        integerVariablesValues = collectVariableValuesData()
    }

    private  fun collectVariableValuesData(): Map<Instruction, Edges<ValuesData>> {
        return pseudocode.collectData(
                TraversalOrder.FORWARD,
                /* mergeDataWithLocalDeclarations */ true,
                { instruction, incomingData: Collection<ValuesData> -> mergeVariablesValues(instruction, incomingData) },
                { i, j, e -> e},
                ValuesData(HashMap(), HashMap())
        )
    }

    private fun mergeVariablesValues(instruction: Instruction, incomingEdgesData: Collection<ValuesData>): Edges<ValuesData> {
        val enterInstructionData = unionIncomingVariablesValues(incomingEdgesData)
        val exitInstructionData = updateValues(instruction, enterInstructionData)
        return Edges(enterInstructionData, exitInstructionData)
    }

    private fun unionIncomingVariablesValues(incomingEdgesData: Collection<ValuesData>): ValuesData {
        val unitedVariables: MutableMap<String, IntegerVariableValues> = HashMap()
        val unitedFakeVariables: MutableMap<String, IntegerVariableValues> = HashMap()
        for(data in incomingEdgesData) {
            mergeCorrespondingVariables(unitedVariables, data.variablesToValues)
            mergeCorrespondingVariables(unitedFakeVariables, data.fakeVariablesToValues)
        }
        return ValuesData(unitedVariables, unitedFakeVariables)
    }

    private fun mergeCorrespondingVariables(
            targetVariablesMap: MutableMap<String, IntegerVariableValues>,
            variablesToConsume: MutableMap<String, IntegerVariableValues>
    ) {
        val unionByKey: (String) -> Unit = { key ->
            val values1 = targetVariablesMap.getOrElse(key, { IntegerVariableValues.empty() })
            val values2 = variablesToConsume.getOrElse(key, { IntegerVariableValues.empty() })
            if(values1.cantBeDefined || values2.cantBeDefined) {
                values1.setUndefined()
            } else {
                values1.addAll(values2)
                targetVariablesMap.put(key, values1)
            }
        }
        val targetMapKeys = HashSet(targetVariablesMap.keySet())
        val mapToConsumeKeys = HashSet(variablesToConsume.keySet())
        for(key in targetMapKeys) unionByKey(key)
        for(key in mapToConsumeKeys) unionByKey(key)
    }

    private fun updateValues(instruction: Instruction, mergedEdgesData: ValuesData): ValuesData {
        val updatedData = ValuesData(HashMap(mergedEdgesData.variablesToValues), HashMap(mergedEdgesData.fakeVariablesToValues))
        when(instruction) {
            is VariableDeclarationInstruction -> {
                // process variable declaration
                val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                               ?: throw Exception("Variable descriptor is null")
                // todo: there should be easier way to define type, for example JetType constants (or consider JetTypeMapper)
                val typeName: String = variableDescriptor.getType().getConstructor().getDeclarationDescriptor()?.getName()?.asString()
                               ?: throw Exception("Variable declaration type name is null")
                if(typeName == "Int") {
                    val declaredName = variableDescriptor.getName().asString()
                    updatedData.variablesToValues.put(declaredName, IntegerVariableValues.empty())
                }
            }
            is ReadValueInstruction -> {
                val element = instruction.outputValue.element
                when (element) {
                    is JetConstantExpression -> {
                        // process literal occurrence (all integer literals are stored to fake variables by read instruction)
                        val node = element.getNode()
                        val nodeType = node.getElementType() as? JetNodeType ?:
                                       throw Exception("Node's elementType has wrong type for JetConstantExpression")
                        if (nodeType == JetNodeTypes.INTEGER_CONSTANT) {
                            val literalValue = Integer.parseInt(node.getText())
                            val fakeVariableName = instruction.outputValue.debugName
                            updatedData.fakeVariablesToValues.put(fakeVariableName, IntegerVariableValues.singleton(literalValue))
                        }
                    }
                    is JetNameReferenceExpression -> {
                        // process variable reference
                        val referencedVariableName = element.getReferencedName()
                        val referencedVariableValues = updatedData.variablesToValues.getOrElse(referencedVariableName, { null })
                        if(referencedVariableValues != null) {
                            // we have the information about value, so it is definitely of integer type
                            // (assuming there are no undeclared variables)
                            val newFakeVariableName = instruction.outputValue.debugName
                            updatedData.fakeVariablesToValues.put(newFakeVariableName, referencedVariableValues)
                        }
                    }
                }
            }
            is WriteValueInstruction -> {
                // process assignment to variable
                val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                         ?: throw Exception("Variable descriptor is null")
                val fakeVariableName = instruction.rValue.debugName
                val valuesToAssign = updatedData.fakeVariablesToValues.get(fakeVariableName)
                val assignmentTarget = variableDescriptor.getName().asString()
                if(valuesToAssign != null) {
                    updatedData.variablesToValues.put(assignmentTarget, valuesToAssign)
                } else {
                    updatedData.variablesToValues.put(assignmentTarget, IntegerVariableValues.cantBeDefined())
                }
            }
            is CallInstruction -> {
                if(instruction.element is JetBinaryExpression) {
                    processBinaryArithmeticOperation(instruction.element.getOperationToken(), instruction, updatedData)
                }
            }
            is MagicInstruction -> {
                when(instruction.kind) {
                    MagicKind.LOOP_RANGE_ITERATION -> {
                        // process range operator result storing in fake variable
                        assert(instruction.inputValues.size() == 1, "Loop range iteration is assumed to have 1 input value")
                        val rangeValuesFakeVariableName = instruction.inputValues.get(0).debugName
                        val rangeValues = updatedData.fakeVariablesToValues.get(rangeValuesFakeVariableName)
                                          ?: throw Exception("Range values are not computed")
                        val targetName = instruction.outputValue.debugName
                        updatedData.fakeVariablesToValues.put(targetName, rangeValues)
                    }
                }
            }
        }
        return updatedData
    }

    private fun processBinaryArithmeticOperation(token: IElementType, instruction: CallInstruction, updatedData: ValuesData) {
        assert(instruction.inputValues.size().equals(2),
               "Binary expression instruction is supposed to have two input values")
        val leftOperandValues = updatedData.fakeVariablesToValues.get(instruction.inputValues.get(0).debugName)
        val rightOperandValues = updatedData.fakeVariablesToValues.get(instruction.inputValues.get(1).debugName)
        if (leftOperandValues == null || rightOperandValues == null) {
            return
        }
        val resultValue = instruction.outputValue ?: return
        val result: IntegerVariableValues =
            if(!leftOperandValues.areDefined || !rightOperandValues.areDefined) {
                IntegerVariableValues.cantBeDefined()
            } else when (token) {
                JetTokens.PLUS -> applyEachToEach(leftOperandValues, rightOperandValues) { l, r -> l + r }
                JetTokens.MINUS -> applyEachToEach(leftOperandValues, rightOperandValues) { l, r -> l - r }
                JetTokens.MUL -> applyEachToEach(leftOperandValues, rightOperandValues) { l, r -> l * r }
                JetTokens.DIV -> applyEachToEach(leftOperandValues, rightOperandValues) { l, r ->
                    if (rightOperandValues.equals(0)) {
                        throw Exception("OutOfBoundChecker: Division by zero detected")
                    } else {
                        l / r
                    }
                }
                JetTokens.RANGE -> {
                    // we can safely use casts below because of areDefined checks above
                    val minOfLeft = leftOperandValues.getValues().min() as Int
                    val maxOfRight = rightOperandValues.getValues().max() as Int
                    val rangeValues = IntegerVariableValues.empty()
                    for(value in minOfLeft..maxOfRight) {
                        rangeValues.add(value)
                    }
                    rangeValues
                }
                else -> throw Exception("OutOfBoundChecker: Unsupported binary operation")
            }
        updatedData.fakeVariablesToValues.put(resultValue.debugName, result)
    }

    private fun applyEachToEach(left: IntegerVariableValues, right: IntegerVariableValues, operation: (Int, Int) -> Int)
            : IntegerVariableValues {
        val resultSet = HashSet<Int>()
        for(l in left.getValues()) {
            for(r in right.getValues()) {
                resultSet.add(operation(l, r))
            }
        }
        return IntegerVariableValues.ofCollection(resultSet)
    }
}