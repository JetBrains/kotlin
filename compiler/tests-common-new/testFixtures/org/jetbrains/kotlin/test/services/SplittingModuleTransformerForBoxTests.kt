/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.RegisteredDirectivesBuilder
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_STATE_MACHINE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.WITH_COROUTINES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

/**
 * This transformers is used for transforming test with several files
 *   into test with two modules, the second one containing the last file.
 *
 * Used when the same test sets are run both in a single module mode (as in IrBlackBoxInlineCodegenTest)
 * and in multi-module mode (as in IrCompileKotlinAgainstInlineKotlinTest).
 *
 * If the test is already multimodule, do nothing.
 * If the test is single-module, single-file, do nothing.
 * NOTE: Make sure SplittingTestConfigurator is also added to metaTestConfigurators to skip running such non-split tests.
 */
@TestInfrastructureInternals
class SplittingModuleTransformerForBoxTests(
    private val testServices: TestServices,
) : ModuleStructureTransformer() {
    /**
     * WARNING: `testServices` is not yet fully completed at the moment of `transformModuleStructure()` invocation,
     * so should be accessed with caution (e.g. they don't have initialized `testModuleStructure`).
     */
    override fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure {
        if (moduleStructure.modules.size > 1) {
            // The test is already multimodule, no need to split it into modules further.
            return moduleStructure
        }
        val module = moduleStructure.modules.single()
        val realFiles = module.files.filterNot { it.isAdditional }
        if (realFiles.size < 2) return moduleStructure // Cannot split single-file tests into two modules. SplittingTestConfigurator will skip this test
        val additionalFiles = module.files.filter { it.isAdditional }
        val boxFiles = realFiles.filter { it.originalContent.contains("fun box()") || it.name == "entry.mjs" }
        val secondModuleFile = when (boxFiles.size) {
            1 -> boxFiles.single()
            0 -> error("Codegen test should contain one global `fun box()`")
            else -> boxFiles.singleOrNull { it.originalContent.contains("\nfun box()") }
                ?: error("Too many `fun box()` are defined. Cannot heuristically detect which one is global")
        }
        val firstModuleFiles = realFiles.filter { it != secondModuleFile }
        val firstModule = TestModule(
            name = "lib",
            files = firstModuleFiles + additionalFiles.filterNot {
                // Avoid adding same global symbols, so deserializer
                it.name == "jsCommonTestHelpers.kt" || it.name == "jsMinimalTestHelpers.kt"
            },
            allDependencies = emptyList(),
            module.directives,
            module.languageVersionSettings
        )

        val secondModule = TestModule(
            name = "main",
            files = listOf(secondModuleFile) + additionalFiles.filterNot {
                // Avoid adding same global symbols, so deserializer
                it.name == "CoroutineHelpers.kt" || it.name == "StateMachineChecker.kt" || it.name == "CoroutineUtil.kt"
            }.map { it.copy() },
            allDependencies = listOf(DependencyDescription(firstModule, DependencyKind.Binary, DependencyRelation.FriendDependency)),
            RegisteredDirectivesBuilder(module.directives).apply {
                -CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS
            }.build(),
            module.languageVersionSettings
        )
        testServices.splitStateProvider.hasBeenSplit = true
        return TestModuleStructureImpl(listOf(firstModule, secondModule), moduleStructure.originalTestDataFiles)
    }
}

/*
 * Always use this configurator along with SplittingModuleTransformerForBoxTests, to skip non-splitted tests, and avoid non-splittable tests
 */
class SplittingTestConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::SplitStateProvider))

    override fun shouldSkipTest(): Boolean {
        if (!testServices.splitStateProvider.hasBeenSplit) return true
        val modules = testServices.moduleStructure.modules
        assert(modules.size == 2) // It's expected that SplittingModuleTransformerForBoxTests splits testData to exactly two modules
        val (moduleLib, moduleMain) = modules

        if (moduleLib.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            // Multiplatform tests must not be tested with SplittingModuleTransformerForBoxTests
            return true
        }
        if (moduleLib.targetPlatform(testServices).isJvm()) {
            // `TestConfigurationBuilder.configureForSerialization()` sets IrInlineBodiesHandler, which requires inlines functions to be present
            // Tests without `inline fun` in lib module must be skipped in such test runners
            if (testServices.defaultsProvider.targetBackend == TargetBackend.JVM_IR_SERIALIZE) {
                if (moduleLib.files.none {
                        it.originalContent.replace("suspend", "")
                            .replace("\\s+".toRegex(), " ")
                            .contains("inline fun")
                    }
                ) return true
            }
        }
        return false // Don't skip its execution, since the test has been split, and no counter-patterns found
    }
}

/**
 * This service must be used only to pass information from SplittingModuleTransformerForBoxTests to SplittingTestConfigurator:
 * to reliably skip tests which were not split by SplittingModuleTransformerForBoxTests
 */
internal class SplitStateProvider : TestService {
    var hasBeenSplit = false
}

internal val TestServices.splitStateProvider: SplitStateProvider by TestServices.testServiceAccessor()
