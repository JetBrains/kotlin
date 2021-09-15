/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import java.util.concurrent.ConcurrentHashMap

@JvmInline
internal value class LibrariesCache(
    private val cache: ConcurrentHashMap<KtSourceModule, FirIdeLibrariesSession> = ConcurrentHashMap()
) {
    fun cached(
        moduleSourceInfo: KtSourceModule,
        create: (KtSourceModule) -> FirIdeLibrariesSession
    ): FirIdeLibrariesSession =
        cache.computeIfAbsent(moduleSourceInfo, create)
}
