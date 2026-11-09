/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

// TODO better, plugin-specific name
abstract class TypeResolveService {
    abstract fun resolveUserType(type: FirUserTypeRef): FirResolvedTypeRef
}

@FirExtensionApiInternals
abstract class TypeResolveServiceProvider : FirSessionComponent {
    abstract fun getTypeResolveService(klass: FirClassSymbol<*>): TypeResolveService?
}

@FirExtensionApiInternals
val FirSession.typeResolveServiceProvider: TypeResolveServiceProvider by FirSession.sessionComponentAccessor()
