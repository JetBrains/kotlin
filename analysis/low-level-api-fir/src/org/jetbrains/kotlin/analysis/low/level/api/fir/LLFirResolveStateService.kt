/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirLibraryOrLibrarySourceResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirNotUnderContentRootResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirScriptResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.project.structure.*

class LLFirResolveSessionService(project: Project) {
    private val cache = LLFirSessionCache.getInstance(project)

    fun getFirResolveSession(module: KtModule): LLFirResolveSession {
        return create(module, cache::getSession)
    }

    @TestOnly
    fun getFirResolveSessionForBinaryModule(module: KtModule): LLFirResolveSession {
        return create(module) { cache.getSession(it, true) }
    }

    fun getFirResolveSessionNoCaching(module: KtModule): LLFirResolveSession {
        return create(module, cache::getSessionNoCaching)
    }

    private fun create(module: KtModule, factory: (KtModule) -> LLFirSession): LLFirResolvableResolveSession {
        return when (module) {
            is KtSourceModule -> LLFirSourceResolveSession(module, factory)
            is KtLibraryModule, is KtLibrarySourceModule -> LLFirLibraryOrLibrarySourceResolvableResolveSession(module, factory)
            is KtScriptModule -> LLFirScriptResolveSession(module, factory)
            is KtNotUnderContentRootModule -> LLFirNotUnderContentRootResolveSession(module, factory)
            else -> {
                errorWithFirSpecificEntries("Unexpected ${module::class.java}") {
                    withEntry("module", module) { it.moduleDescription }
                }
            }
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirResolveSessionService =
            project.getService(LLFirResolveSessionService::class.java)
    }
}