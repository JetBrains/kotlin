/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag

fun ConeClassLikeLookupTag.getNestedClassifierScope(session: FirSession, scopeSession: ScopeSession): FirContainingNamesAwareScope? {
    val klass = toSymbol(session)?.fir as? FirRegularClass ?: return null
    return klass.scopeProvider.getNestedClassifierScope(klass, session, scopeSession)
}
