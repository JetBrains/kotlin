/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.RegisteredDirectivesBuilder
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_STATE_MACHINE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.WITH_COROUTINES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_SPLITTING_TO_TWO_MODULES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.SERIALIZE_IR
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
class SplittingModuleTransformerForBoxTests : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure {
        if (moduleStructure.modules.size > 1) {
            // The test is already multimodule, no need to split it into modules further.
            return moduleStructure
        }
        val module = moduleStructure.modules.single()
        val realFiles = module.files.filterNot { it.isAdditional }
        if (realFiles.size < 2) {
            // Cannot split single-file tests, so cannot split into modules.
            return moduleStructure
        }
        val additionalFiles = module.files.filter { it.isAdditional }
        val firstModuleFiles = realFiles.dropLast(1)
        val secondModuleFile = realFiles.last()
        val firstModule = TestModule(
            name = "lib",
            files = firstModuleFiles + additionalFiles,
            allDependencies = emptyList(),
            module.directives,
            module.languageVersionSettings
        )

        val secondModule = TestModule(
            name = "main",
            files = listOf(secondModuleFile) + additionalFiles.map { it.copy() },
            allDependencies = listOf(DependencyDescription(firstModule, DependencyKind.Binary, DependencyRelation.FriendDependency)),
            RegisteredDirectivesBuilder(module.directives).apply {
                -CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS
            }.build(),
            module.languageVersionSettings
        )
        return TestModuleStructureImpl(listOf(firstModule, secondModule), moduleStructure.originalTestDataFiles)
    }
}

/*
 * Always use this configurator along with SplittingModuleTransformerForBoxTests, to skip non-splitted tests, and avoid non-splittable tests
 */
class SplittingTestConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val modules = testServices.moduleStructure.modules
        if (modules.size != 2) return true

        val (moduleLib, moduleMain) = modules

        val settings = moduleLib.languageVersionSettings
        if (settings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            // Multiplatform tests must not be tested with SplittingModuleTransformerForBoxTests
            return true
        }
        val isJvm = moduleLib.targetPlatform(testServices).isJvm()
        if (isJvm) {
            // `TestConfigurationBuilder.configureForSerialization()` sets IrInlineBodiesHandler, which requires inlines functions to present
            // Tests without `inline` substring in lib module must be skipped in such testrunners
            if (JvmSerializeIrMode.INLINE in moduleLib.directives[SERIALIZE_IR]) {
                if (moduleLib.files.none { it.originalContent.contains("inline") })
                    return true
            }
        }
        if (WITH_COROUTINES in moduleLib.directives && !isJvm) {
            // WITH_COROUTINES works incorrectly for non-jvm multi-module tests, should EmptyContinuation object be referenced from moduleLib,
            // or CHECK_STATE_MACHINE would add `val StateMachineChecker` to both modules
            // Same helper sources `CoroutineHelpers.kt` and `CoroutineUtil.kt` are added to each module, which causes symbols clash in deserialization phase for moduleMain like:
            //   IrClassSymbolImpl is already bound. Signature: helpers/EmptyContinuation|null[0]
            if (CHECK_STATE_MACHINE in moduleLib.directives)
                return true
            if (moduleLib.files[0].originalContent.contains("EmptyContinuation"))
                return true
        }

        val skippedBackends = moduleLib.directives[SKIP_SPLITTING_TO_TWO_MODULES]
        if (testServices.defaultsProvider.targetBackend in skippedBackends ||
            TargetBackend.ANY in skippedBackends
        ) return true

        // The following matcher should recognize the module structure created by SplittingModuleTransformerForBoxTests
        // Some small amount of false-positives are ok, for ex, in `mangling/internal.kt`, where second module has friend dep: `// MODULE: main()(lib)`
        val looksLikeSplitted = moduleMain.friendDependencies.singleOrNull()?.dependencyModule == moduleLib
                && moduleMain.regularDependencies.isEmpty()
                && moduleMain.dependsOnDependencies.isEmpty()
                && moduleLib.friendDependencies.isEmpty()
                && moduleLib.regularDependencies.isEmpty()
                && moduleLib.dependsOnDependencies.isEmpty()
        return !looksLikeSplitted
    }
}
