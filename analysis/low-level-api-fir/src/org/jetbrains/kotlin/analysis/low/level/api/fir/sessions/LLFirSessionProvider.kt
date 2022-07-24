/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.Immutable
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirDependentModuleProviders
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider

@Immutable
class LLFirSessionProvider internal constructor(
    val project: Project,
    internal val rootModuleSession: LLFirResolvableModuleSession,
    private val ktModuleToSession: Map<KtModule, LLFirSession>
) : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): LLFirSession {
        requireIsInstance<LLFirModuleData>(moduleData)
        return getResolvableSession(moduleData.ktModule)
    }

    fun getSession(module: KtModule): LLFirSession =
        ktModuleToSession.getValue(module)

    fun getResolvableSession(module: KtModule): LLFirResolvableModuleSession =
        ktModuleToSession.getValue(module) as LLFirResolvableModuleSession

    val allSessions: Collection<LLFirSession>
        get() = ktModuleToSession.values
}
