/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.Immutable
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache

@Immutable
class FirIdeSessionProvider internal constructor(
    val project: Project,
    internal val rootModuleSession: FirIdeSourcesSession,
    val sessions: Map<ModuleSourceInfoBase, FirIdeSession>
) : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): FirSession? =
        sessions[moduleData.moduleSourceInfo]

    fun getSession(moduleInfo: ModuleInfo): FirSession? =
        sessions[moduleInfo]

    internal fun getModuleCache(moduleSourceInfo: ModuleSourceInfoBase): ModuleFileCache =
        (sessions.getValue(moduleSourceInfo) as FirIdeSourcesSession).cache
}
