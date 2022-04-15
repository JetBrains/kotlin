/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.getValue
import org.jetbrains.kotlin.analysis.utils.caches.softCachedValue
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import java.util.concurrent.ConcurrentHashMap


class LLFirScopeSessionProvider(project: Project) {
    // ScopeSession is thread-local, so we use Thread id as a key
    // We cannot use thread locals here as it may lead to memory leaks
    private val cache by softCachedValue(
        project,
        PsiModificationTracker.MODIFICATION_COUNT,
        ProjectRootModificationTracker.getInstance(project),
    ) {
        ConcurrentHashMap<Long, ScopeSession>()
    }

    fun getScopeSession(): ScopeSession {
        return cache.getOrPut(Thread.currentThread().id) { ScopeSession() }
    }
}