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

import org.jetbrains.kotlin.cfg.outofbound.MapUtils
import org.jetbrains.kotlin.cfg.outofbound.PseudocodeIntegerVariablesDataCollector
import org.jetbrains.kotlin.cfg.outofbound.ValuesData
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.ConfigurationKind

public abstract class AbstractVariableValuesTest : AbstractPseudocodeTest() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL)
    }

    override fun dumpInstructions(pseudocode: PseudocodeImpl, out: StringBuilder, bindingContext: BindingContext) {
        val integerVariableValues = PseudocodeIntegerVariablesDataCollector(pseudocode, bindingContext).collectVariableValuesData()
        if(integerVariableValues.isEmpty()) {
            return
        }
        val inPrefix = "    in: "
        val outPrefix = "   out: "

        val inValuesData = integerVariableValues.mapValues { it.value.incoming }
        val outValuesData = integerVariableValues.mapValues { it.value.outgoing }
        val inDumpData = dumpCollectedData(inValuesData)
        val outDumpData = dumpCollectedData(outValuesData)

        dumpInstructions(pseudocode, out, { instruction, next, prev ->
            val inInstructionDump = inDumpData.data[instruction]
            val outInstructionDump = outDumpData.data[instruction]
            inPrefix +
            "%1$-${inDumpData.intsColumnWidth}s ".format(inInstructionDump?.intVariablesDataString) +
            "%1$-${inDumpData.boolsColumnWidth}s ".format(inInstructionDump?.boolVariablesDataString) +
            "%1$-${inDumpData.arraysColumnWidth}s ".format(inInstructionDump?.arrayVariablesDataString) +
            outPrefix +
            "%1$-${outDumpData.intsColumnWidth}s ".format(outInstructionDump?.intVariablesDataString) +
            "%1$-${outDumpData.boolsColumnWidth}s ".format(outInstructionDump?.boolVariablesDataString) +
            "%1$-${outDumpData.arraysColumnWidth}s ".format(outInstructionDump?.arrayVariablesDataString)
        })
    }

    private data class CollectedDataStrings(
            val intVariablesDataString: String,
            val boolVariablesDataString: String,
            val arrayVariablesDataString: String
    )

    private data class DumpData(
            val data: Map<Instruction, CollectedDataStrings>,
            val intsColumnWidth: Int,
            val boolsColumnWidth: Int,
            val arraysColumnWidth: Int
    )

    private fun dumpCollectedData(dataMap: Map<Instruction, ValuesData>): DumpData {
        val descriptorToString: (VariableDescriptor) -> String = { it.name.asString() }
        val data = dataMap.mapValues {
            CollectedDataStrings(
                    "INTS${MapUtils.mapToString(it.value.intVarsToValues, descriptorToString, descriptorToString)}",
                    "BOOLS${MapUtils.mapToString(it.value.boolVarsToValues, descriptorToString, descriptorToString)}",
                    "ARRS${MapUtils.mapToString(it.value.arraysToSizes, descriptorToString, descriptorToString)}"
            )
        }
        return DumpData(
                data = data,
                intsColumnWidth = data.map { it.value.intVariablesDataString.length() }.max() as Int,
                boolsColumnWidth = data.map { it.value.boolVariablesDataString.length() }.max() as Int,
                arraysColumnWidth = data.map { it.value.arrayVariablesDataString.length() }.max() as Int
        )
    }
}