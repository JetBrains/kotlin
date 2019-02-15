/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirScope

interface FirScopeProvider {
    fun getDeclaredMemberScope(klass: FirRegularClass, session: FirSession): FirScope

    companion object {
        val emptyScope = object : FirScope {}
    }
}