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
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
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

class TypeResolutionConfiguration private constructor(
    val scopes: Iterable<FirScope>,
    val containingClassDeclarations: List<FirClass>,
    // Note: sometimes we don't have useSiteFile in IDE context
    val useSiteFile: FirFile?,
    val topContainer: FirDeclaration? = null,
    // For `x: MySealed` and `x is MySubClass` instead of `x is MySealed.MySubClass`
    // the class symbol would point to MySealed.
    // It's guaranteed to be sealed.
    val sealedClassForContextSensitiveResolution: FirRegularClassSymbol? = null,
) {
    constructor(
        scopes: Iterable<FirScope>,
        containingClassDeclarations: List<FirClass>,
        useSiteFile: FirFile?,
        topContainer: FirDeclaration? = null,
    ) : this(scopes, containingClassDeclarations, useSiteFile, topContainer, sealedClassForContextSensitiveResolution = null)

    companion object {
        fun createForContextSensitiveResolution(
            containingClassDeclarations: List<FirClass>,
            useSiteFile: FirFile,
            topContainer: FirDeclaration?,
            sealedClassForContextSensitiveResolution: FirRegularClassSymbol,
        ): TypeResolutionConfiguration {
            require(sealedClassForContextSensitiveResolution.isSealed)
            return TypeResolutionConfiguration(
                scopes = emptyList(),
                containingClassDeclarations, useSiteFile, topContainer,
                sealedClassForContextSensitiveResolution,
            )
        }
    }
}

data class FirTypeResolutionResult(
    val type: ConeKotlinType,
    val diagnostic: ConeDiagnostic?,
)

val FirSession.typeResolver: FirTypeResolver by FirSession.sessionComponentAccessor()
