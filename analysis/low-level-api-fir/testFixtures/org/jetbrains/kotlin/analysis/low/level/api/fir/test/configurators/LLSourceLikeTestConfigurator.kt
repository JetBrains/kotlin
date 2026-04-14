/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.services.ReplSnippetCompilationService
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtModuleWithModifiableDependencies
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtSourceLikeTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ReplSnippetCompiler
import org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.configureLibraryCompilationSupport
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator.Companion.defaultTargetPlatformValue
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isKtsFile

/**
 * A universal test configurator for source-like tests that use FIR.
 */
open class LLSourceLikeTestConfigurator(
    analyseInDependentSession: Boolean = false,
    override val defaultTargetPlatform: TargetPlatform = defaultTargetPlatformValue,
) : LLSourceLikeBaseTestConfigurator(analyseInDependentSession) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)

        builder.apply {
            useAdditionalService<KtTestModuleFactory> { KtSourceLikeTestModuleFactory }
            useAdditionalService<ReplSnippetCompiler>(::ReplSnippetCompilationService)

            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(DependencyKindModuleStructureTransformer)

            configureLibraryCompilationSupport()
        }
    }

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure {
        val result = super.createModules(moduleStructure, testServices, project)

        val replSnippetModules = result.mainModules.filter { module ->
            module.testModule.files.any { it.isKtsFile && it.name.endsWith(".repl.kts") }
        }

        val librarySnippets = replSnippetModules.filter { it.moduleKind == TestModuleKind.LibraryBinary }
        if (librarySnippets.isEmpty()) return result

        val sourceSnippet = replSnippetModules.single { it.moduleKind == TestModuleKind.ScriptSource }

        // Add all compiled snippet libraries as regular dependencies (reverse order: N-1, N-2, ..., 1)
        val scriptModule = sourceSnippet.ktModule as KtModuleWithModifiableDependencies
        for (libraryModule in librarySnippets.reversed()) {
            scriptModule.directRegularDependencies.add(libraryModule.ktModule)
        }

        // Keep only the source snippet (+ any non-snippet modules) as main modules
        val mainModules = result.mainModules.filter { it !in librarySnippets }
        val binaryModules = result.binaryModules.toList() + librarySnippets.map { it.ktModule as KaLibraryModule }

        return KtTestModuleStructure(result.testModuleStructure, mainModules, binaryModules)
    }
}
