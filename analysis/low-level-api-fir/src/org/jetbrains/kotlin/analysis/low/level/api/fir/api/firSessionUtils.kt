/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeModuleSession

/**
 * Returns a [GlobalSearchScope] declarations from which [FirSession] knows about
 */
val FirSession.searchScope: GlobalSearchScope
    get() {
        check(this is FirIdeModuleSession)
        return scope
    }