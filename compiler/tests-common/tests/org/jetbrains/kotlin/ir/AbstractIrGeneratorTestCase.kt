/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.getAnnotationsJar
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*

abstract class AbstractIrGeneratorTestCase : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        setupEnvironment(files, javaFilesDir)

        loadMultiFiles(files)
        doTest(wholeFile, files)
    }

    private fun setupEnvironment(files: List<TestFile>, javaFilesDir: File?) {
        val jdkKind = getJdkKind(files)

        val javacOptions = ArrayList<String>(0)
        var addRuntime = false
        var addReflect = false
        for (file in files) {
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_RUNTIME")) {
                addRuntime = true
            }
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_REFLECT")) {
                addReflect = true
            }

            javacOptions.addAll(InTextDirectivesUtils.findListWithPrefixes(file.content, "// JAVAC_OPTIONS:"))
        }

        val configurationKind = when {
            addReflect -> ConfigurationKind.ALL
            addRuntime -> ConfigurationKind.NO_KOTLIN_REFLECT
            else -> ConfigurationKind.JDK_ONLY
        }

        val configuration = createConfiguration(
            configurationKind, jdkKind,
            listOf<File>(getAnnotationsJar()),
            arrayOf(javaFilesDir).filterNotNull(),
            files
        )

        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    protected abstract fun doTest(wholeFile: File, testFiles: List<TestFile>)

    protected fun generateIrModule(ignoreErrors: Boolean = false, shouldGenerate: (KtFile) -> Boolean = { true }): IrModuleFragment {
        assert(myFiles != null) { "myFiles not initialized" }
        assert(myEnvironment != null) { "myEnvironment not initialized" }
        return generateIrModule(myFiles.psiFiles, myEnvironment, Psi2IrTranslator(Psi2IrConfiguration(ignoreErrors)), shouldGenerate)
    }

    protected fun generateIrFilesAsSingleModule(testFiles: List<TestFile>, ignoreErrors: Boolean = false): Map<TestFile, IrFile> {
        val irModule = generateIrModule(ignoreErrors)
        val ktFiles = testFiles.filter { it.name.endsWith(".kt") }
        return ktFiles.zip(irModule.files).toMap()
    }

    companion object {
        private val IGNORE_ERRORS_PATTERN = Regex("""// !IGNORE_ERRORS""")

        internal fun shouldIgnoreErrors(wholeFile: File): Boolean =
            IGNORE_ERRORS_PATTERN.containsMatchIn(wholeFile.readText())

        internal fun createExpectedTextFile(testFile: TestFile, dir: File, fileName: String): File {
            val textFile = File(dir, fileName)
            if (!textFile.exists()) {
                TestCase.assertTrue("Can't create an expected text containingFile: ${textFile.absolutePath}", textFile.createNewFile())
                PrintWriter(FileWriter(textFile)).use {
                    it.println("$fileName: new expected text containingFile for ${testFile.name}")
                }
            }
            return textFile
        }

        fun generateIrModule(
            ktFilesToAnalyze: List<KtFile>,
            environment: KotlinCoreEnvironment,
            psi2ir: Psi2IrTranslator,
            shouldGenerate: (KtFile) -> Boolean
        ): IrModuleFragment {
            val analysisResult = JvmResolveUtil.analyze(ktFilesToAnalyze, environment)
            if (!psi2ir.configuration.ignoreErrors) {
                analysisResult.throwIfError()
                AnalyzingUtils.throwExceptionOnErrors(analysisResult.bindingContext)
            }
            val fileToGenerate = ktFilesToAnalyze.filter { shouldGenerate(it) }
            return generateIrModule(fileToGenerate, analysisResult.moduleDescriptor, analysisResult.bindingContext, psi2ir)
        }

        fun generateIrModule(
            ktFiles: List<KtFile>,
            moduleDescriptor: ModuleDescriptor,
            bindingContext: BindingContext,
            ignoreErrors: Boolean = false
        ) =
            generateIrModule(ktFiles, moduleDescriptor, bindingContext, Psi2IrTranslator(Psi2IrConfiguration(ignoreErrors)))

        fun generateIrModule(
            ktFiles: List<KtFile>,
            moduleDescriptor: ModuleDescriptor,
            bindingContext: BindingContext,
            psi2ir: Psi2IrTranslator
        ) =
            psi2ir.generateModule(moduleDescriptor, ktFiles, bindingContext)
    }
}


