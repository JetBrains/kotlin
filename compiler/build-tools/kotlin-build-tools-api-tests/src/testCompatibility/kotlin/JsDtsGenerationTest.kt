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
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_ES_GENERATORS
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
    @TestMetadata("js-ts-generation-no-export")
    @DisplayName("An empty export model produces a declaration file")
    fun emptyExportModelProducesDtsFile(strategyConfig: CompilerExecutionStrategyConfiguration) {
        strategyConfig.first.assumeJsDtsGenerationIsSupported()

        jsProject(strategyConfig) {
            val module = module("js-ts-generation-no-export")
            module.compile()

            val outputDirectory = module.buildDirectory.resolve("dts")
            outputDirectory.createDirectories()
            module.generateDts(outputDirectory = outputDirectory)

            val declarationFiles = outputDirectory.walk().filter { it.isRegularFile() }.toList()
            assertEquals(setOf("js-ts-generation-no-export.d.ts"), declarationFiles.map { it.fileName.toString() }.toSet()) {
                "Unexpected declaration files: $declarationFiles"
            }
            val declarations = declarationFiles.single().readText()
            assertTrue(declarations.contains("KtSingleton"))
            assertTrue(declarations.contains("export as namespace"))
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

    @DisplayName("configureFrom with typed arguments copies boolean options from their arguments")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsCopiesBooleanOptions(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val allOn = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT] = true
            this[X_SUSPEND_LAMBDA_EXPORTING] = true
            this[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = true
            this[X_DTS_USE_UNKNOWN_INSTEAD_ANY] = true
        }
        assertEquals(true, allOn[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])
        assertEquals(true, allOn[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])
        assertEquals(true, allOn[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])
        assertEquals(true, allOn[JsDtsGenerationOperation.USE_UNKNOWN_INSTEAD_ANY])

        val allOff = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT] = false
            this[X_SUSPEND_LAMBDA_EXPORTING] = false
            this[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = false
            this[X_DTS_USE_UNKNOWN_INSTEAD_ANY] = false
        }
        assertEquals(false, allOff[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])
        assertEquals(false, allOff[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])
        assertEquals(false, allOff[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])
        assertEquals(false, allOff[JsDtsGenerationOperation.USE_UNKNOWN_INSTEAD_ANY])
    }

    @DisplayName("configureFrom with command-line arguments copies boolean options from their flags")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineCopiesBooleanOptions(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val allOn = kotlinToolchain.dtsBuilderConfiguredFromCommandLine(
            "-Xenable-implementing-interfaces-from-typescript",
            "-Xsuspend-lambda-exporting",
            "-Xconsistent-data-class-copy-visibility",
            "-Xdts-use-unknown-instead-any",
        )
        assertEquals(true, allOn[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])
        assertEquals(true, allOn[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])
        assertEquals(true, allOn[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])
        assertEquals(true, allOn[JsDtsGenerationOperation.USE_UNKNOWN_INSTEAD_ANY])

        val allOff = kotlinToolchain.dtsBuilderConfiguredFromCommandLine(
            "-Xenable-implementing-interfaces-from-typescript=false",
            "-Xsuspend-lambda-exporting=false",
            "-Xconsistent-data-class-copy-visibility=false",
            "-Xdts-use-unknown-instead-any=false",
        )
        assertEquals(false, allOff[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])
        assertEquals(false, allOff[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])
        assertEquals(false, allOff[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])
        assertEquals(false, allOff[JsDtsGenerationOperation.USE_UNKNOWN_INSTEAD_ANY])
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

    @DisplayName("configureFrom with typed arguments: explicit MODULE_KIND takes precedence over auto-configuration from TARGET")
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

    @DisplayName("configureFrom with typed arguments derives MODULE_KIND from TARGET")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsDerivesModuleKindFromTarget(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val fromEs2015Target = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2015 }
        assertEquals(JsModuleKind.ES, fromEs2015Target[JsDtsGenerationOperation.MODULE_KIND])

        val fromEs2020Target = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2020 }
        assertEquals(JsModuleKind.ES, fromEs2020Target[JsDtsGenerationOperation.MODULE_KIND])
    }

    @DisplayName("configureFrom with typed arguments uses the default MODULE_KIND when neither MODULE_KIND nor TARGET is set")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsFallsBackToUmdModuleKind(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val fromNeither = kotlinToolchain.dtsBuilderConfiguredFrom { }
        assertEquals(JsModuleKind.UMD, fromNeither[JsDtsGenerationOperation.MODULE_KIND])
    }

    @DisplayName("configureFrom with command-line arguments takes MODULE_KIND from -module-kind, otherwise derives it from -target")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineResolvesModuleKind(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val noArguments = kotlinToolchain.dtsBuilderConfiguredFromCommandLine()
        assertEquals(JsModuleKind.UMD, noArguments[JsDtsGenerationOperation.MODULE_KIND])

        val explicitWithTarget = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-module-kind", "commonjs", "-target", "es2015")
        assertEquals(JsModuleKind.COMMONJS, explicitWithTarget[JsDtsGenerationOperation.MODULE_KIND])

        val es2015 = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2015")
        assertEquals(JsModuleKind.ES, es2015[JsDtsGenerationOperation.MODULE_KIND])

        val es2020 = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2020")
        assertEquals(JsModuleKind.ES, es2020[JsDtsGenerationOperation.MODULE_KIND])
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

    @DisplayName("configureFrom with command-line arguments takes GRANULARITY from -Xir-per-file and -Xir-per-module")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineTakesGranularity(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()

        val noArguments = kotlinToolchain.dtsBuilderConfiguredFromCommandLine()
        assertEquals(JsDtsGranularity.WHOLE_PROGRAM, noArguments[JsDtsGenerationOperation.GRANULARITY])

        val perFile = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-Xir-per-file")
        assertEquals(JsDtsGranularity.PER_FILE, perFile[JsDtsGenerationOperation.GRANULARITY])

        val perModule = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-Xir-per-module")
        assertEquals(JsDtsGranularity.WHOLE_PROGRAM, perModule[JsDtsGenerationOperation.GRANULARITY])
    }

    @DisplayName("configureFrom with typed arguments: explicit X_ES_LONG_AS_BIGINT takes precedence over auto-configuration from TARGET")
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

    @DisplayName("configureFrom with typed arguments derives COMPILE_LONG_AS_BIG_INT from TARGET")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsDerivesLongAsBigIntFromTarget(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()
        kotlinToolchain.assumeJsDtsConfigureFromArgumentResolutionIsSupported()

        val es2020 = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2020 }
        assertEquals(true, es2020[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])

        val es2015 = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2015 }
        assertEquals(false, es2015[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
    }

    @DisplayName("configureFrom with typed arguments uses the default COMPILE_LONG_AS_BIG_INT when neither X_ES_LONG_AS_BIGINT nor TARGET is set")
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

    @DisplayName("configureFrom with command-line arguments derives COMPILE_LONG_AS_BIG_INT from -target")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineDerivesLongAsBigIntFromTarget(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()
        kotlinToolchain.assumeJsDtsConfigureFromArgumentResolutionIsSupported()

        val es2020 = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2020")
        assertEquals(true, es2020[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])

        val es2015 = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2015")
        assertEquals(false, es2015[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])

        val noTarget = kotlinToolchain.dtsBuilderConfiguredFromCommandLine()
        assertEquals(false, noTarget[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
    }

    @DisplayName("configureFrom with typed arguments takes COMPILE_SUSPEND_AS_JS_GENERATOR from X_ES_GENERATORS, otherwise derives it from TARGET")
    @BtaVersionsOnlyCompilationTest
    fun configureFromTypedArgumentsResolvesSuspendAsJsGenerator(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()
        kotlinToolchain.assumeJsDtsConfigureFromArgumentResolutionIsSupported()

        val noTarget = kotlinToolchain.dtsBuilderConfiguredFrom { }
        assertEquals(false, noTarget[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val explicitTrue = kotlinToolchain.dtsBuilderConfiguredFrom { this[X_ES_GENERATORS] = true }
        assertEquals(true, explicitTrue[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val explicitFalse = kotlinToolchain.dtsBuilderConfiguredFrom {
            this[TARGET] = JsEcmaVersion.ES2020
            this[X_ES_GENERATORS] = false
        }
        assertEquals(false, explicitFalse[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val es2020 = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2020 }
        assertEquals(true, es2020[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val es2015 = kotlinToolchain.dtsBuilderConfiguredFrom { this[TARGET] = JsEcmaVersion.ES2015 }
        assertEquals(true, es2015[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])
    }

    @DisplayName("configureFrom with command-line arguments takes COMPILE_SUSPEND_AS_JS_GENERATOR from -Xes-generators, otherwise derives it from -target")
    @BtaVersionsOnlyCompilationTest
    fun configureFromCommandLineResolvesSuspendAsJsGenerator(kotlinToolchain: KotlinToolchains) {
        kotlinToolchain.assumeJsDtsGenerationIsSupported()
        kotlinToolchain.assumeJsDtsConfigureFromArgumentResolutionIsSupported()

        val noTarget = kotlinToolchain.dtsBuilderConfiguredFromCommandLine()
        assertEquals(false, noTarget[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val explicitTrue = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-Xes-generators")
        assertEquals(true, explicitTrue[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val explicitFalse = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2015", "-Xes-generators=false")
        assertEquals(false, explicitFalse[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val es2020 = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2020")
        assertEquals(true, es2020[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])

        val es2015 = kotlinToolchain.dtsBuilderConfiguredFromCommandLine("-target", "es2015")
        assertEquals(true, es2015[JsDtsGenerationOperation.COMPILE_SUSPEND_AS_JS_GENERATOR])
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
