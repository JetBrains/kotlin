/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.Immutable
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionProvider

@Immutable
class FirIdeSessionProvider internal constructor(
    val project: Project,
    internal val rootModuleSession: FirIdeSourcesSession,
    val sessions: Map<KtSourceModule, FirIdeSession>
) : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): FirSession? =
        sessions[moduleData.module]

    fun getSession(module: KtModule): FirSession? =
        sessions[module]

    internal fun getModuleCache(module: KtSourceModule): ModuleFileCache =
        (sessions.getValue(module) as FirIdeSourcesSession).cache
}
