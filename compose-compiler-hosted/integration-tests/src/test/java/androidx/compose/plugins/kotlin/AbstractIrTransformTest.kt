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
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.ir.backend.jvm.serialization.EmptyLoggingContext
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

abstract class ComposeIrTransformTest : AbstractIrTransformTest() {
    val extension = ComposeIrGenerationExtension()
    override fun postProcessingStep(
        module: IrModuleFragment,
        generatorContext: GeneratorContext,
        irLinker: IrDeserializer
    ) {
        extension.generate(
            module,
            IrPluginContextImpl(
                generatorContext.moduleDescriptor,
                generatorContext.bindingContext,
                generatorContext.languageVersionSettings,
                generatorContext.symbolTable,
                generatorContext.typeTranslator,
                generatorContext.irBuiltIns,
                irLinker
            )
        )
    }
}

abstract class AbstractIrTransformTest : AbstractCompilerTest() {
    protected fun sourceFile(name: String, source: String): KtFile {
        val result = createFile(name, source, myEnvironment!!.project)
        val ranges = AnalyzingUtils.getSyntaxErrorRanges(result)
        assert(ranges.isEmpty()) { "Syntax errors found in $name: $ranges" }
        return result
    }

    protected open val additionalPaths = emptyList<File>()

    abstract fun postProcessingStep(
        module: IrModuleFragment,
        generatorContext: GeneratorContext,
        irLinker: IrDeserializer
    )

    fun verifyComposeIrTransform(
        source: String,
        expectedTransformed: String,
        extra: String = "",
        dumpTree: Boolean = false
    ) {
        val files = listOf(
            sourceFile("Test.kt", source.replace('%', '$')),
            sourceFile("Extra.kt", extra.replace('%', '$'))
        )
        val irModule = generateIrModuleWithJvmResolve(files)
        val actualTransformed = irModule
            .files[0]
            .dumpSrc()
            .replace('$', '%')
            // replace source keys for start group calls
            .replace(
                Regex(
                    "(%composer\\.start(Restart|Movable|Replaceable)Group\\()([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
            // replace source keys for joinKey calls
            .replace(
                Regex(
                    "(%composer\\.joinKey\\()([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
            // composableLambdaInstance(<>, true)
            .replace(
                Regex(
                    "(composableLambdaInstance\\()([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
            // composableLambda(%composer, <>, true)
            .replace(
                Regex(
                    "(composableLambda\\(%composer,\\s)([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
                // %composer, -1234,
            .replace(
                Regex(
                    "%composer,\\s([-\\d]+),"
                )
            ) {
                "%composer, <>,"
            }
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

    protected fun generateIrModuleWithJvmResolve(files: List<KtFile>): IrModuleFragment {
        val classPath = createClasspath() + additionalPaths
        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)

        val environment = KotlinCoreEnvironment.createForTests(
            myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).also { setupEnvironment(it) }

        val mangler = JvmManglerDesc(null)
        val signaturer = JvmIdSignatureDescriptor(mangler)

        val psi2ir = Psi2IrTranslator(
            environment.configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors = false),
            signaturer
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
        val stubGenerator = DeclarationStubGenerator(
            generatorContext.moduleDescriptor,
            generatorContext.symbolTable,
            generatorContext.irBuiltIns.languageVersionSettings,
            extensions
        )
        val functionFactory = IrFunctionFactory(
            generatorContext.irBuiltIns,
            generatorContext.symbolTable
        )
        generatorContext.irBuiltIns.functionFactory = functionFactory
        val irLinker = JvmIrLinker(
            generatorContext.moduleDescriptor,
            EmptyLoggingContext,
            generatorContext.irBuiltIns,
            generatorContext.symbolTable,
            functionFactory,
            stubGenerator,
            mangler
        )

        generatorContext.moduleDescriptor.allDependencyModules.map {
            val capability = it.getCapability(KlibModuleOrigin.CAPABILITY)
            val kotlinLibrary = (capability as? DeserializedKlibModuleOrigin)?.library
            irLinker.deserializeIrModuleHeader(it, kotlinLibrary)
        }

        val irProviders = listOf(irLinker)

        stubGenerator.setIrProviders(irProviders)

        ExternalDependenciesGenerator(
            generatorContext.symbolTable,
            irProviders,
            generatorContext.languageVersionSettings
        ).generateUnboundSymbolsAsDependencies()

        psi2ir.addPostprocessingStep { module ->
            val old = stubGenerator.unboundSymbolGeneration
            try {
                stubGenerator.unboundSymbolGeneration = true
                postProcessingStep(module, generatorContext, irLinker)
            } finally {
                stubGenerator.unboundSymbolGeneration = old
            }
        }

        val irModuleFragment = psi2ir.generateModuleFragment(
            generatorContext,
            files,
            irProviders,
            IrGenerationExtension.getInstances(myEnvironment!!.project),
            expectDescriptorToSymbol = null
        )
        irLinker.postProcess()
        return irModuleFragment
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
