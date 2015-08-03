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

import com.google.common.collect.Maps
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.CallInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.JetArrayAccessExpression
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

public class OutOfBoundChecker(val pseudocode: Pseudocode, val trace: BindingTrace) {
    private val arrayGetFunctionName = "get"
    private val arraySetFunctionName = "set"

    private val pseudocodeVariablesDataCollector: PseudocodeIntegerVariablesDataCollector =
            PseudocodeIntegerVariablesDataCollector(pseudocode, trace.bindingContext)

    public fun checkOutOfBoundErrors() {
        val outOfBoundAnalysisData = pseudocodeVariablesDataCollector.collectVariableValuesData()
        val reportedDiagnosticMap = Maps.newHashMap<Instruction, DiagnosticFactory<*>>()
        pseudocode.traverse(TraversalOrder.FORWARD, outOfBoundAnalysisData, { instruction, inData: ValuesData, outData: ValuesData ->
            val ctxt = VariableContext(instruction, reportedDiagnosticMap)
            if (instruction is CallInstruction &&
               (isArrayGetCall(instruction) || isArraySetCall(instruction))) {
                checkOutOfBoundAccess(instruction, inData, ctxt)
            }
        })
    }

    private fun isArrayGetCall(instruction: CallInstruction): Boolean {
        val isGetFunctionCall = instruction.element is JetCallExpression &&
                                JetExpressionUtils.tryGetCalledName(instruction.element)?.let { it == arrayGetFunctionName } ?: false
        val isGetOperation = (isGetFunctionCall || instruction.element is JetArrayAccessExpression) &&
                             instruction.inputValues.size() == 2
        return isGetOperation && receiverIsArray(instruction)
    }

    private fun isArraySetCall(instruction: CallInstruction): Boolean {
        val isSetFunctionCall = instruction.element is JetCallExpression &&
                                JetExpressionUtils.tryGetCalledName(instruction.element)?.let { it == arraySetFunctionName } ?: false
        val isSetThroughAccessOperation = instruction.element is JetBinaryExpression &&
                                          instruction.element.left is JetArrayAccessExpression
        val isSetOperation = (isSetFunctionCall || isSetThroughAccessOperation) &&
                             instruction.inputValues.size() == 3
        return isSetOperation && receiverIsArray(instruction)
    }

    private fun receiverIsArray(instruction: CallInstruction): Boolean {
        val callReceiver = instruction.resolvedCall.dispatchReceiver
        return if (callReceiver != ReceiverValue.NO_RECEIVER)
            KotlinCodeUtils.isGenericOrPrimitiveArray(callReceiver.type)
        else false
    }

    private fun checkOutOfBoundAccess(instruction: CallInstruction, valuesData: ValuesData, ctxt: VariableContext) {
        val arraySizeVariable = instruction.inputValues[0]
        val accessPositionVariable = instruction.inputValues[1]
        val arraySizes = valuesData.intFakeVarsToValues[arraySizeVariable]
        val accessPositions = valuesData.intFakeVarsToValues[accessPositionVariable]
        if (arraySizes != null && accessPositions != null &&
            arraySizes.isDefined && accessPositions.isDefined) {
            val sizes = arraySizes.getValues().sort()
            val positions = accessPositions.getValues().sort()
            if (sizes.first() <= positions.last()) {
                report(Errors.OUT_OF_BOUND_ACCESS.on(instruction.element, sizes.first(), positions.last()), ctxt)
            }
            else if (positions.first() < 0) {
                report(Errors.OUT_OF_BOUND_ACCESS.on(instruction.element, sizes.first(), positions.first()), ctxt)
            }
        }
    }

    // ======= all the code below is copied (and adapted) from org.jetbrains.kotlin.cfg.JetFlowInformationProvider ======
    // it should be reused when this class will be merged with JetFlowInformationProvider
    private fun report(
            diagnostic: Diagnostic,
            ctxt: VariableContext) {
        val instruction = ctxt.instruction
        if (instruction.copies.isEmpty()) {
            trace.report(diagnostic)
            return
        }
        val previouslyReported = ctxt.reportedDiagnosticMap
        previouslyReported.put(instruction, diagnostic.getFactory())

        var alreadyReported = false
        var sameErrorForAllCopies = true
        for (copy in instruction.copies) {
            val previouslyReportedErrorFactory = previouslyReported.get(copy)
            if (previouslyReportedErrorFactory != null) {
                alreadyReported = true
            }

            if (previouslyReportedErrorFactory !== diagnostic.getFactory()) {
                sameErrorForAllCopies = false
            }
        }

        //only one reporting required
        if (!alreadyReported) {
            trace.report(diagnostic)
        }
    }

    private inner class VariableContext (
            val instruction: Instruction,
            val reportedDiagnosticMap: MutableMap<Instruction, DiagnosticFactory<*>>) {
        val variableDescriptor: VariableDescriptor? = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true, trace.getBindingContext())
    }
}