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

import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleImpl
import org.jetbrains.kotlin.psi2ir.IrGeneratorContext
import org.jetbrains.kotlin.psi2ir.IrModuleGenerator
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import java.io.File

abstract class AbstractIrGeneratorTestCase : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, files, javaFilesDir)
        loadMultiFiles(files)
        doTest(wholeFile, files)
    }

    protected abstract fun doTest(wholeFile: File, testFiles: List<TestFile>)

    protected fun generateIrFilesAsSingleModule(testFiles: List<TestFile>): Map<TestFile, IrFile> {
        assert(myFiles != null) { "myFiles not initialized" }
        assert(myEnvironment != null) { "myEnvironment not initialized" }
        val analysisResult = JvmResolveUtil.analyzeAndCheckForErrors(myFiles.psiFiles, myEnvironment)
        analysisResult.throwIfError()
        AnalyzingUtils.throwExceptionOnErrors(analysisResult.bindingContext)
        val irModule = IrModuleImpl(analysisResult.moduleDescriptor)
        val irGeneratorContext = IrGeneratorContext(myFiles.psiFiles, irModule, analysisResult.bindingContext)
        IrModuleGenerator(irGeneratorContext).generateModuleContent()
        return testFiles.filter { it.name.endsWith(".kt") }.zip(irModule.files).toMap()
    }
}


