/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirScope

class JavaFirScopeProvider : FirScopeProvider {
    override fun getDeclaredMemberScope(klass: FirRegularClass, session: FirSession): FirScope {
        if (klass !is FirJavaClass) return FirScopeProvider.emptyScope

        return JavaClassEnhancementScope(session, klass.useSiteScope)
    }
}