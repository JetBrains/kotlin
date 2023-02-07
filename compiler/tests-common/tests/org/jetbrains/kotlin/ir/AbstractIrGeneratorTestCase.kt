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

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.NameProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.psi2ir.generators.generateTypicalIrProviderList
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.util.KtTestUtil.getAnnotationsJar
import java.io.File

abstract class AbstractIrGeneratorTestCase : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        setupEnvironment(files)

        loadMultiFiles(files)
        doTest(wholeFile, files)
    }

    private fun setupEnvironment(files: List<TestFile>) {
        val jdkKind = getTestJdkKind(files)

        val javacOptions = ArrayList<String>(0)
        var addRuntime = false
        var addReflect = false
        for (file in files) {
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_STDLIB")) {
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
            configurationKind, jdkKind, backend,
            listOf<File>(getAnnotationsJar()),
            listOfNotNull(writeJavaFiles(files)),
            files
        )

        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    protected abstract fun doTest(wholeFile: File, testFiles: List<TestFile>)

    protected open fun generateIrModule(ignoreErrors: Boolean = false): IrModuleFragment {
        assert(myFiles != null) { "myFiles not initialized" }
        assert(myEnvironment != null) { "myEnvironment not initialized" }
        val psi2Ir = Psi2IrTranslator(
            myEnvironment.configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors),
            myEnvironment.configuration::checkNoUnboundSymbols
        )
        return doGenerateIrModule(psi2Ir)
    }

    protected open fun doGenerateIrModule(psi2IrTranslator: Psi2IrTranslator): IrModuleFragment =
        generateIrModuleWithJvmResolve(
            myFiles.psiFiles,
            myEnvironment,
            psi2IrTranslator,
            myEnvironment.configuration.languageVersionSettings
        )

    protected fun generateIrFilesAsSingleModule(testFiles: List<TestFile>, ignoreErrors: Boolean = false): Map<TestFile, IrFile> {
        val irModule = generateIrModule(ignoreErrors)
        val ktFiles = testFiles.filter { it.name.endsWith(".kt") }
        return ktFiles.zip(irModule.files).toMap()
    }

    companion object {
        private val IGNORE_ERRORS_PATTERN = Regex("""// !IGNORE_ERRORS""")

        internal fun shouldIgnoreErrors(wholeFile: File): Boolean =
            IGNORE_ERRORS_PATTERN.containsMatchIn(wholeFile.readText())

        fun generateIrModuleWithJsResolve(
            ktFilesToAnalyze: List<KtFile>, environment: KotlinCoreEnvironment, psi2ir: Psi2IrTranslator
        ): IrModuleFragment =
            generateIrModule(
                TopDownAnalyzerFacadeForJS.analyzeFiles(
                    ktFilesToAnalyze, environment.project, environment.configuration,
                    moduleDescriptors = emptyList(),
                    friendModuleDescriptors = emptyList(),
                    CompilerEnvironment,
                ),
                psi2ir, ktFilesToAnalyze, GeneratorExtensions(),
                createIdSignatureComposer = { IdSignatureDescriptor(JsManglerDesc) }
            )

        fun generateIrModuleWithJvmResolve(
            ktFilesToAnalyze: List<KtFile>,
            environment: KotlinCoreEnvironment,
            psi2ir: Psi2IrTranslator,
            languageVersionSettings: LanguageVersionSettings
        ): IrModuleFragment {
            return generateIrModule(
                JvmResolveUtil.analyze(ktFilesToAnalyze, environment), psi2ir, ktFilesToAnalyze,
                JvmGeneratorExtensionsImpl(environment.configuration, generateFacades = false),
                createIdSignatureComposer = { bindingContext ->
                    JvmIdSignatureDescriptor(JvmDescriptorMangler(MainFunctionDetector(bindingContext, languageVersionSettings)))
                }
            )
        }

        private fun generateIrModule(
            analysisResult: AnalysisResult,
            psi2ir: Psi2IrTranslator,
            ktFilesToAnalyze: List<KtFile>,
            generatorExtensions: GeneratorExtensions,
            createIdSignatureComposer: (BindingContext) -> IdSignatureComposer
        ): IrModuleFragment {
            val (bindingContext, moduleDescriptor) = analysisResult
            if (!psi2ir.configuration.ignoreErrors) {
                analysisResult.throwIfError()
                AnalyzingUtils.throwExceptionOnErrors(bindingContext)
            }
            val context = psi2ir.createGeneratorContext(
                moduleDescriptor,
                bindingContext,
                // SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImpl, NameProvider.DEFAULT),
                SymbolTable(createIdSignatureComposer(bindingContext), IrFactoryImpl, NameProvider.DEFAULT),
                generatorExtensions
            )
            val irProviders = generateTypicalIrProviderList(
                moduleDescriptor, context.irBuiltIns, context.symbolTable,
                DescriptorByIdSignatureFinderImpl(moduleDescriptor, JsManglerDesc),
                extensions = generatorExtensions,
            )
            return psi2ir.generateModuleFragment(context, ktFilesToAnalyze, irProviders, emptyList())
        }
    }
}
