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
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.CallInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.BindingTrace
import java.util.HashMap

// Performs Pseudocode checks based on variable values analysis
public class VariableValuesBasedChecker(
        val pseudocode: Pseudocode,
        val trace: BindingTrace,
        val report: (Diagnostic, Instruction, HashMap<Instruction, DiagnosticFactory<*>>) -> Unit
) {
    private val pseudocodeVariablesDataCollector: PseudocodeIntegerVariablesDataCollector =
            PseudocodeIntegerVariablesDataCollector(pseudocode, trace.bindingContext)
    private val variableValuesData: Map<Instruction, Edges<ValuesData>> =
            pseudocodeVariablesDataCollector.collectVariableValuesData()

    // Checks if there are out-of-bound-access errors in the `pseudocode` and reports them using `report` function
    public fun checkOutOfBoundErrors() {
        val reportedDiagnosticMap = Maps.newHashMap<Instruction, DiagnosticFactory<*>>()
        pseudocode.traverse(TraversalOrder.FORWARD, variableValuesData, { instruction, inData: ValuesData, outData: ValuesData ->
            if (instruction is CallInstruction) {
                val pseudoAnnotation = CallInstructionUtils.tryExtractPseudoAnnotationForAccess(instruction)
                if (pseudoAnnotation != null) {
                    checkOutOfBoundAccess(instruction, inData, reportedDiagnosticMap)
                }
            }
        })
    }

    // Returns `true` if the passed `instruction` is unreachable according to variable values analysis
    public fun isUnreachableAccordingValueAnalysis(instruction: Instruction): Boolean =
            variableValuesData[instruction]?.incoming is ValuesData.Dead

    private fun checkOutOfBoundAccess(
            instruction: CallInstruction,
            valuesData: ValuesData,
            reportedDiagnosticMap: HashMap<Instruction, DiagnosticFactory<*>>
    ) {
        if (valuesData is ValuesData.Defined) {
            val arraySizeVariable = instruction.inputValues[0]
            val accessPositionVariable = instruction.inputValues[1]
            val arraySizes = valuesData.intFakeVarsToValues[arraySizeVariable]
            val accessPositions = valuesData.intFakeVarsToValues[accessPositionVariable]
            if (arraySizes != null && accessPositions != null &&
                arraySizes is IntegerVariableValues.Defined && accessPositions is IntegerVariableValues.Defined) {
                val sizes = arraySizes.values.sort()
                val positions = accessPositions.values.sort()
                if (sizes.first() <= positions.last()) {
                    report(Errors.OUT_OF_BOUND_ACCESS.on(instruction.element, sizes.first(), positions.last()), instruction, reportedDiagnosticMap)
                }
                else if (positions.first() < 0) {
                    report(Errors.OUT_OF_BOUND_ACCESS.on(instruction.element, sizes.first(), positions.first()), instruction, reportedDiagnosticMap)
                }
            }
        }
    }
}