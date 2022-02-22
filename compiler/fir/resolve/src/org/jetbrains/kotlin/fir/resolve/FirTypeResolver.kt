/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef

abstract class FirTypeResolver : FirSessionComponent {
    abstract fun resolveType(
        typeRef: FirTypeRef,
        scopeClassDeclaration: ScopeClassDeclaration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        // Note: sometimes we don't have useSiteFile in IDE context
        useSiteFile: FirFile?,
        supertypeSupplier: SupertypeSupplier
    ): Pair<ConeKotlinType, ConeDiagnostic?>
}

val FirSession.typeResolver: FirTypeResolver by FirSession.sessionComponentAccessor()
