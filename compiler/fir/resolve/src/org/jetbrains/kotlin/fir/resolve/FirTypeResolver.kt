/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef

abstract class FirTypeResolver : FirSessionComponent {
    abstract fun resolveType(
        typeRef: FirTypeRef,
        configuration: TypeResolutionConfiguration,
        // TODO: Consider putting other parameters to TypeResolutionConfiguration
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: SupertypeSupplier,
        expandTypeAliases: Boolean = true,
    ): FirTypeResolutionResult
}

class TypeResolutionConfiguration(
    val scopes: Iterable<FirScope>,
    val containingClassDeclarations: List<FirClass>,
    // Note: sometimes we don't have useSiteFile in IDE context
    val useSiteFile: FirFile?,
    val topContainer: FirDeclaration? = null,
)

data class FirTypeResolutionResult(
    val type: ConeKotlinType,
    val diagnostic: ConeDiagnostic?,
)

val FirSession.typeResolver: FirTypeResolver by FirSession.sessionComponentAccessor()
