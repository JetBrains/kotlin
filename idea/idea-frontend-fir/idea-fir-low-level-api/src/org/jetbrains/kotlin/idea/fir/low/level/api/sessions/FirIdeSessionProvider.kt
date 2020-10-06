/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.isLibraryClasses
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.Immutable
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache

@Immutable
class FirIdeSessionProvider internal constructor(
    override val project: Project,
    internal val rootModuleSession: FirIdeSourcesSession,
    val sessions: Map<ModuleSourceInfo, FirIdeSession>
) : FirSessionProvider {
    override fun getSession(moduleInfo: ModuleInfo): FirSession? =
        sessions[moduleInfo]

    internal fun getModuleCache(moduleSourceInfo: ModuleSourceInfo): ModuleFileCache =
        (sessions.getValue(moduleSourceInfo) as FirIdeSourcesSession).cache
}
