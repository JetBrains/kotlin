/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionProvider
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.project.isLibraryClasses
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.addToStdlib.cast

private fun createLibrarySession(moduleInfo: IdeaModuleInfo, project: Project, provider: FirProjectSessionProvider): FirLibrarySession {
    val contentScope = moduleInfo.contentScope()
    return FirLibrarySession.create(moduleInfo, provider, contentScope, project, IDEPackagePartProvider(contentScope))
}

interface FirResolveState {
    val sessionProvider: FirSessionProvider

    fun getSession(psi: KtElement): FirSession {
        val sessionProvider = sessionProvider as FirProjectSessionProvider
        val moduleInfo = psi.getModuleInfo() as ModuleSourceInfo
        return sessionProvider.getSession(moduleInfo) ?: FirIdeJavaModuleBasedSession(
            psi.project, moduleInfo, sessionProvider, moduleInfo.contentScope()
        ).also {
            val ideaModuleInfo = moduleInfo.cast<IdeaModuleInfo>()
            ideaModuleInfo.dependenciesWithoutSelf().forEach {
                if (it is IdeaModuleInfo && it.isLibraryClasses()) {
                    createLibrarySession(it, psi.project, sessionProvider)
                }
            }
        }
    }

    operator fun get(psi: KtElement): FirElement?

    fun record(psi: KtElement, fir: FirElement)
}

class FirResolveStateImpl(override val sessionProvider: FirSessionProvider) : FirResolveState {
    private val cache = mutableMapOf<KtElement, FirElement>()

    override fun get(psi: KtElement): FirElement? = cache[psi]

    override fun record(psi: KtElement, fir: FirElement) {
        cache[psi] = fir
    }
}

// TODO: caching
object FirIdeResolveFactory {
    fun initiate(psi: KtElement): FirResolveState {
        val provider = FirProjectSessionProvider(psi.project)
        return FirResolveStateImpl(provider)
    }
}

fun KtElement.firResolveState(): FirResolveState = FirIdeResolveFactory.initiate(this)