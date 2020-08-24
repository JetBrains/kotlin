/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.idea.fir.low.level.api.FirIdeSessionProvider

internal abstract class FirIdeSession(sessionProvider: FirIdeSessionProvider) : FirSession(sessionProvider) {
    abstract val scope: GlobalSearchScope
}