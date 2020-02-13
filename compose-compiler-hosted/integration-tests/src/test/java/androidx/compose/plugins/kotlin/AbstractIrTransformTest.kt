/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import androidx.compose.plugins.kotlin.compiler.lower.dumpSrc
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendFacade
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

abstract class AbstractIrTransformTest : AbstractCompilerTest() {
    private fun sourceFile(name: String, source: String): KtFile {
        val result = createFile(name, source, myEnvironment!!.project)
        val ranges = AnalyzingUtils.getSyntaxErrorRanges(result)
        assert(ranges.isEmpty()) { "Syntax errors found in $name: $ranges" }
        return result
    }

    protected open val additionalPaths = emptyList<File>()

    fun verifyComposeIrTransform(
        transforms: Int,
        source: String,
        expectedTransformed: String,
        extra: String = "",
        dumpTree: Boolean = false
    ) {
        val files = listOf(
            sourceFile("Test.kt", source.replace('%', '$')),
            sourceFile("Extra.kt", extra.replace('%', '$'))
        )
        val irModule = generateIrModuleWithJvmResolve(
            files,
            transforms
        )
        val actualTransformed = irModule
            .files[0]
            .dumpSrc()
            .replace('$', '%')
            .trimIndent()
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
        if (dumpTree) {
            println(irModule.dump())
        }
        assertEquals(
            expectedTransformed
                .trimIndent()
                .trimTrailingWhitespacesAndAddNewlineAtEOF(),
            actualTransformed
        )
    }

    private fun generateIrModuleWithJvmResolve(
        files: List<KtFile>,
        transforms: Int
    ): IrModuleFragment {
        val classPath = createClasspath() + additionalPaths
        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)

        val environment = KotlinCoreEnvironment.createForTests(
            myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).also { setupEnvironment(it) }

        val psi2ir = Psi2IrTranslator(
            environment.configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors = false)
        )

        val analysisResult = JvmResolveUtil.analyze(files, environment)
        if (!psi2ir.configuration.ignoreErrors) {
            analysisResult.throwIfError()
            AnalyzingUtils.throwExceptionOnErrors(analysisResult.bindingContext)
        }
        val extensions = JvmGeneratorExtensions()
        val generatorContext = psi2ir.createGeneratorContext(
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            extensions = extensions
        )

        val irProviders = generateTypicalIrProviderList(
            analysisResult.moduleDescriptor,
            generatorContext.irBuiltIns,
            generatorContext.symbolTable,
            extensions = extensions
        )

        ExternalDependenciesGenerator(
            generatorContext.symbolTable,
            irProviders
        ).generateUnboundSymbolsAsDependencies()

        val extension = ComposeIrGenerationExtension()

        psi2ir.addPostprocessingStep { module ->
            extension.generate(
                module,
                IrPluginContext(
                    generatorContext.moduleDescriptor,
                    generatorContext.bindingContext,
                    generatorContext.languageVersionSettings,
                    generatorContext.symbolTable,
                    generatorContext.typeTranslator,
                    generatorContext.irBuiltIns,
                    irProviders = irProviders
                ),
                transforms = transforms
            )
        }

        return psi2ir.generateModuleFragment(
            generatorContext,
            files,
            irProviders = irProviders,
            expectDescriptorToSymbol = null
        )
    }

    fun facadeClassGenerator(source: DeserializedContainerSource): IrClass? {
        val jvmPackagePartSource = source.safeAs<JvmPackagePartSource>() ?: return null
        val facadeName = jvmPackagePartSource.facadeClassName ?: jvmPackagePartSource.className
        return buildClass {
            origin = IrDeclarationOrigin.FILE_CLASS
            name = facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName()
        }.also {
            it.createParameterDeclarations()
        }
    }
}
