/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.Immutable
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.NoCacheForModuleException
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionProvider

@Immutable
class LLFirSessionProvider internal constructor(
    val project: Project,
    internal val rootModuleSession: LLFirResolvableModuleSession,
    private val moduleToSession: Map<KtModule, LLFirResolvableModuleSession>
) : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): FirSession? =
        moduleToSession[moduleData.module]

    fun getSession(module: KtModule): FirSession? =
        moduleToSession[module]

    internal fun getModuleCache(module: KtModule): ModuleFileCache =
        moduleToSession[module]?.cache
            ?: throw NoCacheForModuleException(module, moduleToSession.keys)

    val allSessions: Collection<LLFirModuleSession>
        get() = moduleToSession.values
}
