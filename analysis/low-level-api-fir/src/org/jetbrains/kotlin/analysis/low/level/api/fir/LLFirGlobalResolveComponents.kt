/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirScopeSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider

@Suppress("unused")
internal class LLFirGlobalResolveComponents(
    val useSiteKtModule: KtModule,
    val project: Project,
) {
    internal val phaseRunner: LLFirPhaseRunner = LLFirPhaseRunner()
    internal val lockProvider: LockProvider<FirFile> = LockProvider()
}

@Suppress("CanBeParameter")
internal class LLFirModuleResolveComponents(
    val module: KtModule,
    val globalResolveComponents: LLFirGlobalResolveComponents,
    val scopeProvider: FirScopeProvider,
    val cache: ModuleFileCache,
) {
    init {
        cache.moduleComponents = this
    }

    val firFileBuilder: FirFileBuilder = FirFileBuilder(this)
    val lazyFirDeclarationsResolver = FirLazyDeclarationResolver(this)
    val scopeSessionProvider: LLFirScopeSessionProvider = LLFirScopeSessionProvider(globalResolveComponents.project)
    val fileStructureCache: FileStructureCache = FileStructureCache(this)
    val elementsBuilder = FirElementBuilder(this)
    val diagnosticsCollector = DiagnosticsCollector(fileStructureCache)

    lateinit var session: LLFirResolvableModuleSession
}