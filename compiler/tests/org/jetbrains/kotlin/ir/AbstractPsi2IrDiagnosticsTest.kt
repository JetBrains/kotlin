/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractPsi2IrDiagnosticsTest : AbstractDiagnosticsTest() {
    override fun performAdditionalChecksAfterDiagnostics(
            testDataFile: File,
            testFiles: MutableList<TestFile>,
            moduleFiles: MutableMap<TestModule?, MutableList<TestFile>>,
            moduleDescriptors: MutableMap<TestModule?, ModuleDescriptorImpl>,
            moduleBindings: MutableMap<TestModule?, BindingContext>
    ) {
        val actualIrDump = StringBuilder()
        for (module in moduleFiles.keys) {
            val ktFiles = moduleFiles[module]?.mapNotNull { it.jetFile } ?: continue
            val moduleDescriptor = moduleDescriptors[module] ?: continue
            val moduleBindingContext = moduleBindings[module] ?: continue
            val irModule = AbstractIrGeneratorTestCase.generateIrModule(ktFiles, moduleDescriptor, moduleBindingContext, true)
            actualIrDump.append(irModule.dump())
        }

        val expectedIrFile = File(testDataFile.canonicalPath.replace(".kt", ".ir.txt"))
        KotlinTestUtils.assertEqualsToFile(expectedIrFile, actualIrDump.toString())
    }
}
