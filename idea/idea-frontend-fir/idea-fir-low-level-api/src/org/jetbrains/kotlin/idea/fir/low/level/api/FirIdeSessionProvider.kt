/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
internal class FirIdeSessionProvider(project: Project) : FirProjectSessionProvider(project) {
    override val sessionCache = ConcurrentHashMap<ModuleInfo, FirSession>()

    fun getSession(psi: KtElement): FirSession {
        val moduleInfo = psi.getModuleInfo()
        return getSession(moduleInfo)
    }

    override fun getSession(moduleInfo: ModuleInfo): FirSession {
        return sessionCache[moduleInfo]
            ?: error("$moduleInfo")
    }
}
