/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider

/**
 * [org.jetbrains.kotlin.fir.FirSession] responsible for all Kotlin & Java source modules analysing module transitively depends on
 */
@OptIn(PrivateSessionConstructor::class)
internal class FirIdeSourcesSession @PrivateSessionConstructor constructor(
    moduleInfo: ModuleInfo,
    sessionProvider: FirIdeSessionProvider,
    override val scope: GlobalSearchScope,
    val firFileBuilder: FirFileBuilder,
) : FirIdeSession(moduleInfo, sessionProvider) {
    val cache get() = firIdeProvider.cache
}
