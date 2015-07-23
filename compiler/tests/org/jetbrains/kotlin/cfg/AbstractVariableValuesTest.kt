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

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cfg.outofbound.OutOfBoundChecker
import org.jetbrains.kotlin.cfg.outofbound.PseudocodeIntegerVariablesDataCollector
import org.jetbrains.kotlin.cfg.outofbound.ValuesData
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

public abstract class AbstractVariableValuesTest : AbstractPseudocodeTest() {
    override fun dumpInstructions(pseudocode: PseudocodeImpl, out: StringBuilder, bindingContext: BindingContext) {
        val integerVariableValues = PseudocodeIntegerVariablesDataCollector(pseudocode, bindingContext).integerVariablesValues
        val inPrefix = "    in: "
        val outPrefix = "   out: "

        val inValuesData = integerVariableValues.mapValues { it.value.incoming }
        val outValuesData = integerVariableValues.mapValues { it.value.outgoing }
        val inDumpData = dumpValues(inValuesData)
        val outDumpData = dumpValues(outValuesData)

        dumpInstructions(pseudocode, out, { instruction, next, prev ->
            val inValues = inDumpData.data[instruction]
            val outValues = outDumpData.data[instruction]
            inPrefix +
            "%1$-${inDumpData.intsColumnWidth}s ".format(inValues?.get(0)) +
            "%1$-${inDumpData.fakeIntsColumnWidth}s ".format(inValues?.get(1)) +
            "%1$-${inDumpData.boolsColumnWidth}s ".format(inValues?.get(2)) +
            "%1$-${inDumpData.fakeBoolsColumnWidth}s ".format(inValues?.get(3)) +
            outPrefix +
            "%1$-${outDumpData.intsColumnWidth}s ".format(outValues?.get(0)) +
            "%1$-${outDumpData.fakeIntsColumnWidth}s ".format(outValues?.get(1)) +
            "%1$-${outDumpData.boolsColumnWidth}s ".format(outValues?.get(2)) +
            "%1$-${outDumpData.fakeBoolsColumnWidth}s ".format(outValues?.get(3))
        })
    }

    private data class DumpData(
            val data: Map<Instruction, List<String>>,
            val intsColumnWidth: Int,
            val fakeIntsColumnWidth: Int,
            val boolsColumnWidth: Int,
            val fakeBoolsColumnWidth: Int
    )

    private fun dumpValues(dataMap: Map<Instruction, ValuesData>): DumpData {
        val data = dataMap.mapValues {
            val res = it.value.toString().split("|")
            assert(res.size() == 4, "ValuesData has incorrect values format")
            res
        } // todo: why \\s is not recognized as any whitespace?
        return DumpData(
                data = data,
                intsColumnWidth = data.map { it.value[0].length() }.max() as Int,
                fakeIntsColumnWidth = data.map { it.value[1].length() }.max() as Int,
                boolsColumnWidth = data.map { it.value[2].length() }.max() as Int,
                fakeBoolsColumnWidth = data.map { it.value[3].length() }.max() as Int
        )
    }
}