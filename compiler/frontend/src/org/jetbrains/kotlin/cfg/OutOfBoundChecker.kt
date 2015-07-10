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

import org.jetbrains.kotlin.JetNodeType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.ReadValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import java.util.*
import kotlin.properties.Delegates

public class OutOfBoundCheckerState(val subroutine: JetElement, val trace: BindingTrace) {
    val pseudocode: Pseudocode = JetControlFlowProcessor(trace).generatePseudocode(subroutine)
    val pseudocodeVariablesData:PseudocodeVariablesData
            by Delegates.lazy { PseudocodeVariablesData(pseudocode, trace.getBindingContext()); }
}

private fun tryGetReferenceExprName(expression: JetExpression?) =
        (expression as? JetNameReferenceExpression)?.getReferencedName()

private fun tryGetAssignmentName(instruction: MagicInstruction): String? =
    when(instruction.element) {
        is JetProperty -> instruction.element.getName()
        is JetBinaryExpression -> tryGetReferenceExprName(instruction.element.getLeft())
        else -> null
    }

fun checkOutOfBoundErrors(state: OutOfBoundCheckerState) {
    val initializers = state.pseudocodeVariablesData.getVariableInitializers()

    val variablesToValues = HashMap<String, Int>()
    val fakeVariablesToValues = HashMap<String, Int>()

    val processInstruction: (Instruction,
                             Map<VariableDescriptor, PseudocodeVariablesData.VariableInitState>?,
                             Map<VariableDescriptor, PseudocodeVariablesData.VariableInitState>?) -> Unit = {
        instruction, input, output
        ->
        when(instruction) {
            is VariableDeclarationInstruction -> {
                // process variable declaration
                val declaration = instruction.variableDeclarationElement
                variablesToValues.put(declaration.getName(), null)
            }
            is ReadValueInstruction -> {
                // process literal occurrence (all integer literals are stored to fake vars by read instruction)
                val element = instruction.outputValue.element
                if (element is JetConstantExpression) {
                    val node = element.getNode()
                    val nodeType = node.getElementType() as JetNodeType
                    if (nodeType == JetNodeTypes.INTEGER_CONSTANT) {
                        fakeVariablesToValues.put(instruction.outputValue.debugName, Integer.parseInt(node.getText()))
                    }
                }
            }
            is MagicInstruction -> {
                when(instruction.kind) {
                    MagicKind.UNSUPPORTED_ELEMENT -> {
                        // process assignment to variable
                        assert(instruction.inputValues.size().equals(1), "Assignment instruction is supposed to have one input value")
                        val valueToAssign = fakeVariablesToValues.get(instruction.inputValues.get(0).debugName)
                        val assignmentTarget = tryGetAssignmentName(instruction)
                        if(assignmentTarget != null) {
                            variablesToValues.put(assignmentTarget, valueToAssign)
                        }
                    }
                    MagicKind.UNRESOLVED_CALL -> {
                        when(instruction.element) {
                            is JetNameReferenceExpression -> {
                                // process variable reference
                                val referencedVariableName = instruction.element.getReferencedName()
                                val referencedVariableValue = variablesToValues.getOrElse(referencedVariableName, { null })
                                if(referencedVariableValue != null) {
                                    // we have the information about value, so it is definitely of integer type
                                    fakeVariablesToValues.put(instruction.outputValue.debugName, referencedVariableValue)
                                }
                            }
                            is JetBinaryExpression -> {
                                // process binary arithmetic operation
                                assert(instruction.inputValues.size().equals(2),
                                       "Binary expression instruction is supposed to have two input values")
                                val leftOperand = fakeVariablesToValues.get(instruction.inputValues.get(0).debugName)
                                val rightOperand = fakeVariablesToValues.get(instruction.inputValues.get(1).debugName)
                                val result = when(instruction.element.getOperationToken()) {
                                    JetTokens.PLUS -> leftOperand + rightOperand
                                    JetTokens.MINUS -> leftOperand - rightOperand
                                    JetTokens.MUL -> leftOperand * rightOperand
                                    JetTokens.DIV ->
                                            if (rightOperand.equals(0)) {
                                                throw Exception("OutOfBoundChecker: Division by zero detected")
                                            } else {
                                                leftOperand / rightOperand
                                            }
                                    else -> throw Exception("OutOfBoundChecker: Unsupported binary operation")
                                }
                                fakeVariablesToValues.put(instruction.outputValue.debugName, result)
                            }
                        }
                    }
                }
            }
        }
    }
    state.pseudocode.traverse(TraversalOrder.FORWARD, initializers, processInstruction)
}