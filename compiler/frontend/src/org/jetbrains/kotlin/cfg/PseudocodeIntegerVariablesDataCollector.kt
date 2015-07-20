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
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
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

public data class ValuesData(
        val intVarsToValues: MutableMap<VariableDescriptor, IntegerVariableValues> = HashMap(),
        val intFakeVarsToValues: MutableMap<PseudoValue, IntegerVariableValues> = HashMap(),
        val boolVarsToValues: MutableMap<VariableDescriptor, BooleanVariableValue> = HashMap(),
        val boolFakeVarsToValues: MutableMap<PseudoValue, BooleanVariableValue> = HashMap()
)

public class PseudocodeIntegerVariablesDataCollector(
        val pseudocode: Pseudocode,
        val bindingContext: BindingContext
) {
    val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)

    public var integerVariablesValues: Map<Instruction, Edges<ValuesData>> = collectVariableValuesData()
        private set

    public fun recollectIntegerVariablesValues() {
        integerVariablesValues = collectVariableValuesData()
    }

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
                { previous, current, edgeData -> removeOutOfScopeVariables(previous, current, edgeData) },
                ValuesData(HashMap(), HashMap(), HashMap(), HashMap())
        )
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
        return ValuesData(
                HashMap(filteredIntVars),
                HashMap(filteredIntFakeVars),
                HashMap(filteredBoolVars),
                HashMap(filteredBoolFakeVars)
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

    private fun mergeVariablesValues(instruction: Instruction, incomingEdgesData: Collection<ValuesData>): Edges<ValuesData> {
        val enterInstructionData = unionIncomingVariablesValues(incomingEdgesData)
        val exitInstructionData = updateValues(instruction, enterInstructionData)
        return Edges(enterInstructionData, exitInstructionData)
    }

    private fun unionIncomingVariablesValues(incomingEdgesData: Collection<ValuesData>): ValuesData {
        val unitedIntVariables: MutableMap<VariableDescriptor, IntegerVariableValues> = HashMap()
        val unitedIntFakeVariables: MutableMap<PseudoValue, IntegerVariableValues> = HashMap()
        val unitedBoolVariables: MutableMap<VariableDescriptor, BooleanVariableValue> = HashMap()
        val unitedBoolFakeVariables: MutableMap<PseudoValue, BooleanVariableValue> = HashMap()
        for (data in incomingEdgesData) {
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
        val unionByKey: (K) -> Unit = { key ->
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
        val updatedData = ValuesData(
                HashMap(mergedEdgesData.intVarsToValues),
                HashMap(mergedEdgesData.intFakeVarsToValues),
                HashMap(mergedEdgesData.boolVarsToValues),
                HashMap(mergedEdgesData.boolFakeVarsToValues)
        )
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
        val typeName = tryGetVariableType(variableDescriptor)
                       ?: throw Exception("Variable type name is null")
        when(typeName) {
            "Int" -> updatedData.intVarsToValues.put(variableDescriptor, IntegerVariableValues.empty())
            "Boolean" -> updatedData.boolVarsToValues.put(variableDescriptor, BooleanVariableValue.undefinedWithNoRestrictions)
        }
    }

    private fun processLiteral(element: JetConstantExpression, instruction: ReadValueInstruction, updatedData: ValuesData) {
        // process literal occurrence (all literals are stored to fake variables by read instruction)
        val node = element.getNode()
        val nodeType = node.getElementType() as? JetNodeType
                       ?: throw Exception("Node's elementType has wrong type for JetConstantExpression")
        val fakeVariable = instruction.outputValue
        val valuesAsText = node.getText()
        when (nodeType) {
            JetNodeTypes.INTEGER_CONSTANT -> {
                val literalValue = Integer.parseInt(valuesAsText)
                updatedData.intFakeVarsToValues.put(fakeVariable, IntegerVariableValues.singleton(literalValue))
            }
            JetNodeTypes.BOOLEAN_CONSTANT -> {
                val booleanValue = Boolean.parseBoolean(valuesAsText)
                updatedData.boolFakeVarsToValues.put(fakeVariable, BooleanVariableValue.trueOrFalse(booleanValue))
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
                updatedData.intFakeVarsToValues.put(newFakeVariable, referencedVariableValue)
            is BooleanVariableValue ->
                // we have the information about value, so it is definitely of boolean type
                // (assuming there are no undeclared variables)
                updatedData.boolFakeVarsToValues.put(newFakeVariable, referencedVariableValue)
        }
    }

    private fun processAssignmentToVariable(instruction: WriteValueInstruction, updatedData: ValuesData) {
        // process assignment to variable
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: throw Exception("Variable descriptor is null")
        val fakeVariable = instruction.rValue
        val targetTypeName = tryGetTargetType(instruction.target)
                             ?: throw Exception("Cannot define target type in assignment")
        when (targetTypeName) {
            "Int" -> {
                val valuesToAssign = updatedData.intFakeVarsToValues.get(fakeVariable)
                if (valuesToAssign != null) {
                    updatedData.intVarsToValues.put(variableDescriptor, valuesToAssign)
                } else {
                    updatedData.intVarsToValues.put(variableDescriptor, IntegerVariableValues.cantBeDefined())
                }
            }
            "Boolean" -> {
                val valueToAssign = updatedData.boolFakeVarsToValues.get(fakeVariable)
                if (valueToAssign != null) {
                    updatedData.boolVarsToValues.put(variableDescriptor, valueToAssign)
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
                operandsContainer: MutableMap<PseudoValue, Op>,
                resultContainer: MutableMap<PseudoValue, R>,
                operation: (Op, Op) -> R
        ) {
            val leftOperandValues = operandsContainer[leftOperandVariable]
            val rightOperandValues = operandsContainer[rightOperandVariable]
            if (leftOperandValues != null && rightOperandValues != null) {
                resultContainer.put(resultVariable, operation(leftOperandValues, rightOperandValues))
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

    private fun tryGetVariableType(descriptor: CallableDescriptor): String? =
        // todo: there should be easier way to define type, for example JetType constants (or consider JetTypeMapper)
        descriptor.getReturnType()?.getConstructor()?.getDeclarationDescriptor()?.getName()?.asString()


    private fun tryGetTargetType(target: AccessTarget): String? {
        return when(target) {
            is AccessTarget.Declaration -> tryGetVariableType(target.descriptor)
            is AccessTarget.Call -> tryGetVariableType(target.resolvedCall.getResultingDescriptor())
            else -> null
        }
    }
}