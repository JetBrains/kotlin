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
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerArguments.Companion.X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerArguments.Companion.X_SUSPEND_LAMBDA_EXPORTING
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_DTS_USE_UNKNOWN_INSTEAD_ANY
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
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
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

    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ts-generation")
    @DisplayName("USE_UNKNOWN_INSTEAD_ANY set on the operation exports Any and dynamic as unknown")
    fun useUnknownInsteadAnyExportsUntypedAsUnknown(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation")
            module.compile()

            val outputDirectory = module.buildDirectory.resolve("dts-unknown")
            module.generateDts(
                outputDirectory = outputDirectory,
                compilationConfigAction = { it[JsDtsGenerationOperation.USE_UNKNOWN_INSTEAD_ANY] = true },
            )

            val dts = outputDirectory.resolve("js-ts-generation.d.ts").readText()
            assertTrue("function takeAny(x: NonNullable<unknown>): NonNullable<unknown>" in dts, dts)
            assertTrue("function takeDynamic(x: unknown): unknown" in dts, dts)
        }
    }

    @DisplayName("configureFrom with typed arguments takes MODULE_KIND from the linking MODULE_KIND, even when TARGET is set")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsTakesExplicitModuleKind(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val fromModuleKind = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[JsCompilerLinkingArguments.MODULE_KIND] = JsModuleKind.COMMONJS
        }
        assertEquals(JsModuleKind.COMMONJS, fromModuleKind[JsDtsGenerationOperation.MODULE_KIND])

        val fromBoth = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[JsCompilerLinkingArguments.MODULE_KIND] = JsModuleKind.COMMONJS
            this[TARGET] = JsEcmaVersion.ES2015
        }
        assertEquals(JsModuleKind.COMMONJS, fromBoth[JsDtsGenerationOperation.MODULE_KIND])
    }

    @DisplayName("configureFrom with typed arguments derives ES MODULE_KIND from TARGET ES2015 or newer")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsDerivesModuleKindFromTarget(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val fromEs2015Target = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2015 }
        assertEquals(JsModuleKind.ES, fromEs2015Target[JsDtsGenerationOperation.MODULE_KIND])

        val fromEs2020Target = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2020 }
        assertEquals(JsModuleKind.ES, fromEs2020Target[JsDtsGenerationOperation.MODULE_KIND])
    }

    @DisplayName("configureFrom with typed arguments falls back to UMD MODULE_KIND when neither MODULE_KIND nor TARGET is set")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsFallsBackToUmdModuleKind(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val fromNeither = kotlinToolchain.dtsBuilderConfiguredFrom { }
        assertEquals(JsModuleKind.UMD, fromNeither[JsDtsGenerationOperation.MODULE_KIND])
    }

    @DisplayName("configureFrom with typed arguments takes GRANULARITY from X_IR_PER_FILE and X_IR_PER_MODULE")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsTakesGranularity(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        // TODO (KT-90681): check TS_COMPILATION_STRATEGY once it is decided how it follows -Xir-per-file
        val perFile = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_IR_PER_FILE] = true }
        assertEquals(JsDtsGranularity.PER_FILE, perFile[JsDtsGenerationOperation.GRANULARITY])

        val perModule = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_IR_PER_MODULE] = true }
        assertEquals(JsDtsGranularity.WHOLE_PROGRAM, perModule[JsDtsGenerationOperation.GRANULARITY])
    }

    @DisplayName("configureFrom with typed arguments copies boolean options from their arguments")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsCopiesBooleanOptions(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val interfaces = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT] = true }
        assertEquals(true, interfaces[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])

        val suspendLambdas = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_SUSPEND_LAMBDA_EXPORTING] = true }
        assertEquals(true, suspendLambdas[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])

        val dataClassCopy = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = true }
        assertEquals(true, dataClassCopy[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])

        val unknownInsteadAny = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_DTS_USE_UNKNOWN_INSTEAD_ANY] = true }
        assertEquals(true, unknownInsteadAny[JsDtsGenerationOperation.USE_UNKNOWN_INSTEAD_ANY])
    }

    @DisplayName("configureFrom with typed arguments takes COMPILE_LONG_AS_BIG_INT from X_ES_LONG_AS_BIGINT, even when TARGET is set")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsTakesExplicitLongAsBigInt(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val explicitTrue = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_ES_LONG_AS_BIGINT] = true }
        assertEquals(true, explicitTrue[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])

        val es2020ExplicitFalse = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[TARGET] = JsEcmaVersion.ES2020
            this[X_ES_LONG_AS_BIGINT] = false
        }
        assertEquals(false, es2020ExplicitFalse[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
    }

    @DisplayName("configureFrom with typed arguments derives COMPILE_LONG_AS_BIG_INT from TARGET: on for ES2020, off for older")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsDerivesLongAsBigIntFromTarget(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()
        kotlinToolchain.assumeJsDtsConfigureFromArgumentResolutionIsSupported()

        val es2020 = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2020 }
        assertEquals(true, es2020[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])

        val es2015 = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2015 }
        assertEquals(false, es2015[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
    }

    @DisplayName("configureFrom with typed arguments leaves COMPILE_LONG_AS_BIG_INT off when neither X_ES_LONG_AS_BIGINT nor TARGET is set")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsLeavesLongAsBigIntOffByDefault(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val noTarget = kotlinToolchain.dtsBuilderConfiguredFrom { }
        assertEquals(false, noTarget[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
    }

    @DisplayName("configureFrom with command-line arguments takes COMPILE_LONG_AS_BIG_INT from -Xes-long-as-bigint with an explicit value")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineTakesExplicitLongAsBigInt(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val explicitTrue = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-Xes-long-as-bigint=true")
        assertEquals(true, explicitTrue[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])

        val es2020ExplicitFalse = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2020", "-Xes-long-as-bigint=false")
        assertEquals(false, es2020ExplicitFalse[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
    }

    @DisplayName("configureFrom with command-line arguments derives COMPILE_LONG_AS_BIG_INT from -target es2020")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineDerivesLongAsBigIntFromTarget(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()
        kotlinToolchain.assumeJsDtsConfigureFromArgumentResolutionIsSupported()

        val es2020 = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2020")
        assertEquals(true, es2020[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
    }

    @DisplayName("configureFrom with command-line arguments takes every option from its flag")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineTakesEveryOption(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        // TODO (KT-90681): check TS_COMPILATION_STRATEGY once it is decided how it follows -Xir-per-file
        // TODO (KT-91152): check COMPILE_SUSPEND_AS_JS_GENERATOR from -Xes-generators
        val builder = kotlinToolchain.dtsBuilderConfiguredFromCommandLine(
            "-module-kind", "commonjs",
            "-Xir-per-file",
            "-Xes-long-as-bigint",
            "-Xenable-implementing-interfaces-from-typescript",
            "-Xsuspend-lambda-exporting",
            "-Xconsistent-data-class-copy-visibility",
            "-Xdts-use-unknown-instead-any",
        )

        assertEquals(JsModuleKind.COMMONJS, builder[JsDtsGenerationOperation.MODULE_KIND])
        assertEquals(JsDtsGranularity.PER_FILE, builder[JsDtsGenerationOperation.GRANULARITY])
        assertEquals(true, builder[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
        assertEquals(true, builder[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])
        assertEquals(true, builder[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])
        assertEquals(true, builder[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])
        assertEquals(true, builder[JsDtsGenerationOperation.USE_UNKNOWN_INSTEAD_ANY])
    }

    @DisplayName("configureFrom with command-line arguments takes boolean options from -XXLanguage features")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineTakesOptionsFromLanguageFeatures(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()
        kotlinToolchain.assumeJsDtsConfigureFromArgumentResolutionIsSupported()

        val interfaces = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-XXLanguage:+JsExportInterfacesInImplementableWay")
        assertEquals(true, interfaces[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])

        val suspendLambdas = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-XXLanguage:+JsExportingSuspendLambdas")
        assertEquals(true, suspendLambdas[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])

        val dataClassCopy = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-XXLanguage:+DataClassCopyRespectsConstructorVisibility")
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

    private fun KotlinToolchains.dtsBuilderConfiguredFromCommandLine(vararg linkingArguments: String): JsDtsGenerationOperation.Builder =
        dtsBuilderConfiguredFrom { applyCommandLineArguments(linkingArguments.toList()) }

    private fun KotlinToolchains.assumeJsDtsConfigureFromArgumentResolutionIsSupported() {
        assumeTrue(KotlinToolingVersion(getCompilerVersion()) >= KotlinToolingVersion(2, 5, 20, "Beta1")) {
            "Resolving JS linking arguments in JsDtsGenerationOperation.Builder.configureFrom is not supported on Kotlin versions before 2.5.20-Beta1"
        }
    }
}
