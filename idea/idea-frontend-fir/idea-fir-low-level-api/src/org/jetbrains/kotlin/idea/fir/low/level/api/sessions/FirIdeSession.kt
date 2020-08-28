/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirModuleBasedSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

@OptIn(PrivateSessionConstructor::class)
internal abstract class FirIdeSession(
    moduleInfo: ModuleInfo,
    sessionProvider: FirIdeSessionProvider
) : FirModuleBasedSession(moduleInfo, sessionProvider) {
    abstract val scope: GlobalSearchScope
}
