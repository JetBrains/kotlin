/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.Immutable
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSessionProvider
import java.lang.ref.WeakReference

@Immutable
class LLFirSessionProvider internal constructor(
    val project: Project,
    internal val rootModuleSession: LLFirResolvableModuleSession,
    private val ktModuleToSession: KtModuleToSessionMapping
) : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): LLFirSession {
        requireIsInstance<LLFirModuleData>(moduleData)
        return getResolvableSession(moduleData.ktModule)
    }

    fun getSession(module: KtModule): LLFirSession =
        ktModuleToSession.getSession(module)

    fun getResolvableSession(module: KtModule): LLFirResolvableModuleSession =
        ktModuleToSession.getSession(module) as LLFirResolvableModuleSession

    @get:TestOnly
    val allSessions: Collection<LLFirSession>
        get() = ktModuleToSession.getAllSessions()
}

internal abstract class KtModuleToSessionMapping {
    abstract fun getSession(module: KtModule): LLFirSession

    @TestOnly
    abstract fun getAllSessions(): Collection<LLFirSession>
}

internal class KtModuleToSessionMappingByMapImpl(
    private val map: Map<KtModule, LLFirSession>
) : KtModuleToSessionMapping() {
    override fun getSession(module: KtModule): LLFirSession =
        map.getValue(module)

    override fun getAllSessions(): Collection<LLFirSession> =
        map.values
}

internal class KtModuleToSessionMappingByWeakValueMapImpl(
    initialMap: Map<KtModule, LLFirSession>
) : KtModuleToSessionMapping() {
    private val softValuesMap = initialMap.entries.associate { (key, value) -> key to WeakReference(value) }

    override fun getSession(module: KtModule): LLFirSession {
        val softReference = softValuesMap.getValue(module)
        return softReference.get()
            ?: error("soft reference for $module was invalidated")
    }

    override fun getAllSessions(): Collection<LLFirSession> =
        softValuesMap.keys.map { getSession(it) }
}