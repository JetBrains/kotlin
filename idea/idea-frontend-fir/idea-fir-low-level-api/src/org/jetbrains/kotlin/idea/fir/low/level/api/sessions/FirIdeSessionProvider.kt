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
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe

@ThreadSafe
internal class FirIdeSessionProvider(
    override val project: Project,
) : FirSessionProvider {
    private lateinit var sourcesSession: FirIdeSourcesSession
    private lateinit var librariesSession: FirIdeLibrariesSession


    fun setSourcesSession(sourcesSession: FirIdeSourcesSession) {
        check(!this::sourcesSession.isInitialized)
        this.sourcesSession = sourcesSession
    }

    fun setLibrariesSession(librariesSession: FirIdeLibrariesSession) {
        check(!this::librariesSession.isInitialized)
        this.librariesSession = librariesSession
    }

    override fun getSession(moduleInfo: ModuleInfo): FirSession = when {
        moduleInfo is IdeaModuleInfo && moduleInfo.isLibraryClasses() -> {
            librariesSession /* TODO check if library is in libraries session scope */
        }
        moduleInfo is ModuleSourceInfo -> {
            sourcesSession /* TODO check if source is in sources session scope */
        }
        else -> error("Invalid module info $moduleInfo")
    }
}
