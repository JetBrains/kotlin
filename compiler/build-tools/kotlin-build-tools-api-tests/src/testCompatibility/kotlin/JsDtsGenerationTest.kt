/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_IR_MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerKlibArguments.Companion.X_IR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerArguments.Companion.X_SUSPEND_LAMBDA_EXPORTING
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_ES_LONG_AS_BIGINT
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_IR_PER_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_IR_PER_MODULE
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.assumeJsDtsGenerationIsSupported
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

@MustRunOnChangesInJs
@OptIn(ExperimentalCompilerArgument::class)
class JsDtsGenerationTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("ES module kind produces .d.mts declarations")
    fun moduleKindEsProducesMjsDeclarations(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")
            module.compile()

            val outputDirectory = module.buildDirectory.resolve("dts-es")
            outputDirectory.createDirectories()
            module.generateDts(
                outputDirectory = outputDirectory,
                compilationConfigAction = { it[JsDtsGenerationOperation.MODULE_KIND] = JsModuleKind.ES },
            ) {
                assertOutputs("js-ts-generation.d.mts")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("Per-file granularity emits declarations for each source file")
    fun perFileGranularitySplitsDeclarations(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")
            module.compile()

            val outputDirectory = module.buildDirectory.resolve("dts-per-file")
            outputDirectory.createDirectories()
            module.generateDts(
                outputDirectory = outputDirectory,
                compilationConfigAction = {
                    it[JsDtsGenerationOperation.GRANULARITY] = JsDtsGranularity.PER_FILE
                    it[JsDtsGenerationOperation.TS_COMPILATION_STRATEGY] = JsDtsCompilationStrategy.EACH_FILE
                },
            )
            val declarationFiles = outputDirectory.walk().filter { it.isRegularFile() }.toList()
            val exportDeclarationNames = declarationFiles
                .map { it.fileName.toString() }
                .filter { it.endsWith(".export.d.ts") }
                .toSet()

            assertEquals(setOf("Api.export.d.ts", "Box.export.d.ts"), exportDeclarationNames) {
                "Unexpected declaration files: $declarationFiles"
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("Declaration file name is taken from IR_OUTPUT_NAME of the KLIB")
    fun outputNameIsTakenFromIrOutputName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")
            module.compile(compilationConfigAction = { it.compilerArguments[IR_OUTPUT_NAME] = "ir-output-name" })

            val outputDirectory = module.buildDirectory.resolve("dts-ir-output-name")
            outputDirectory.createDirectories()
            module.generateDts(outputDirectory = outputDirectory) {
                assertOutputs("ir-output-name.d.ts")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("X_IR_MODULE_NAME of the KLIB takes precedence over IR_OUTPUT_NAME")
    fun outputNameIsTakenFromIrModuleName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")
            module.compile(compilationConfigAction = { it.compilerArguments[X_IR_MODULE_NAME] = "ir-module-name" })

            val outputDirectory = module.buildDirectory.resolve("dts-ir-module-name")
            outputDirectory.createDirectories()
            module.generateDts(outputDirectory = outputDirectory) {
                assertOutputs("ir-module-name.d.ts")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("X_IR_PER_MODULE_OUTPUT_NAME of the KLIB takes precedence over X_IR_MODULE_NAME")
    fun outputNameIsTakenFromPerModuleOutputName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")
            module.compile(compilationConfigAction = {
                it.compilerArguments[X_IR_MODULE_NAME] = "ir-module-name"
                it.compilerArguments[X_IR_PER_MODULE_OUTPUT_NAME] = "per-module-output-name"
            })

            val outputDirectory = module.buildDirectory.resolve("dts-per-module-output-name")
            outputDirectory.createDirectories()
            module.generateDts(outputDirectory = outputDirectory) {
                assertOutputs("per-module-output-name.d.ts")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("Empty KLIB list succeeds without producing declarations")
    fun emptyKlibListSucceedsWithoutOutput(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")

            // TODO (KT-90579): the empty KLIB list behavior is not documented; update the expected result and check the warning once it is decided
            module.generateDts(outputDirectory = module.buildDirectory.resolve("dts-empty"), klibs = emptyList()) {
                assertOutputs()
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("Missing output directory is created")
    fun missingOutputDirectoryIsCreated(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")
            module.compile()

            val outputDirectory = module.buildDirectory.resolve("missing/nested/dts")
            assertFalse(outputDirectory.exists())
            module.generateDts(outputDirectory = outputDirectory) {
                assertOutputs("js-ts-generation.d.ts")
            }
        }
    }

    @DisplayName("configureFrom takes MODULE_KIND from -module-kind and -target")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTakesModuleKind(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val fromModuleKind = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[JsCompilerLinkingArguments.MODULE_KIND] = JsModuleKind.COMMONJS
        }
        assertEquals(JsModuleKind.COMMONJS, fromModuleKind[JsDtsGenerationOperation.MODULE_KIND])

        val fromTarget = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2015 }
        assertEquals(JsModuleKind.ES, fromTarget[JsDtsGenerationOperation.MODULE_KIND])

        val fromBoth = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[JsCompilerLinkingArguments.MODULE_KIND] = JsModuleKind.COMMONJS
            this[TARGET] = JsEcmaVersion.ES2015
        }
        assertEquals(JsModuleKind.COMMONJS, fromBoth[JsDtsGenerationOperation.MODULE_KIND])
    }

    @DisplayName("configureFrom takes GRANULARITY from -Xir-per-file and -Xir-per-module")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTakesGranularity(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        // TODO (KT-90681): check TS_COMPILATION_STRATEGY once it is decided how it follows -Xir-per-file
        val perFile = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_IR_PER_FILE] = true }
        assertEquals(JsDtsGranularity.PER_FILE, perFile[JsDtsGenerationOperation.GRANULARITY])

        val perModule = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_IR_PER_MODULE] = true }
        assertEquals(JsDtsGranularity.WHOLE_PROGRAM, perModule[JsDtsGenerationOperation.GRANULARITY])
    }

    @DisplayName("configureFrom copies declaration flags from the linking arguments")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCopiesDeclarationFlags(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        // TODO (KT-90366): check EXPORT_UNTYPED_AS_UNKNOWN from -Xdts-use-unknown-instead-any
        // TODO (KT-90489): check COMPILE_LONG_AS_BIG_INT derived from the ES2020 target
        // TODO (KT-90519): check the options enabled through -XXLanguage
        // TODO (KT-90528): check IMPLEMENT_INTERFACES once -Xenable-implementing-interfaces-from-typescript is available for linking
        val longAsBigInt = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_ES_LONG_AS_BIGINT] = true }
        assertEquals(true, longAsBigInt[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])

        val suspendLambdas = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_SUSPEND_LAMBDA_EXPORTING] = true }
        assertEquals(true, suspendLambdas[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])

        val dataClassCopy = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = true }
        assertEquals(true, dataClassCopy[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])
    }

    private fun KotlinToolchains.dtsBuilderConfiguredFrom(
        linkingArguments: JsCompilerLinkingArguments.Builder.() -> Unit,
    ): JsDtsGenerationOperation.Builder {
        val linkingOperation = js.jsLinkingOperationBuilder(Paths.get("main.klib"), Paths.get("js")).apply {
            compilerArguments.linkingArguments()
        }.build()
        return js.jsDtsGenerationOperationBuilder(listOf(Paths.get("main.klib")), Paths.get("dts")).apply {
            configureFrom(linkingOperation)
        }
    }
}
