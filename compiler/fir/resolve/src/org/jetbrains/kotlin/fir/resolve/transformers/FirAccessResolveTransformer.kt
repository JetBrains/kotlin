/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypedProjection
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirAccessResolveTransformer : FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority = true) {

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        return withScopeCleanup {
            towerScope.scopes += FirTopLevelDeclaredMemberScope(file, file.session)
            super.transformFile(file, data)
        }
    }

    private fun ConeClassLikeType.buildSubstitutionScope(
        useSiteSession: FirSession,
        unsubstituted: FirScope,
        regularClass: FirRegularClass
    ): FirClassSubstitutionScope? {
        if (this.typeArguments.isEmpty()) return null

        @Suppress("UNCHECKED_CAST")
        val substitution = regularClass.typeParameters.zip(this.typeArguments) { typeParameter, typeArgument ->
            typeParameter.symbol to (typeArgument as? ConeTypedProjection)?.type
        }.filter { (_, type) -> type != null }.toMap() as Map<ConeTypeParameterSymbol, ConeKotlinType>

        return FirClassSubstitutionScope(useSiteSession, unsubstituted, substitution, true)
    }

    private fun FirRegularClass.buildUseSiteScope(useSiteSession: FirSession = session): FirClassUseSiteScope {
        val superTypeScope = FirCompositeScope(mutableListOf())
        val declaredScope = FirClassDeclaredMemberScope(this, useSiteSession)
        lookupSuperTypes(this, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
            .mapNotNullTo(superTypeScope.scopes) { useSiteSuperType ->
                if (useSiteSuperType is ConeClassErrorType) return@mapNotNullTo null
                val symbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession)
                if (symbol is FirClassSymbol) {
                    val scope = symbol.fir.buildUseSiteScope(useSiteSession)
                    useSiteSuperType.buildSubstitutionScope(useSiteSession, scope, symbol.fir) ?: scope
                } else {
                    null
                }
            }
        return FirClassUseSiteScope(useSiteSession, superTypeScope, declaredScope, true)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            towerScope.scopes += regularClass.buildUseSiteScope()
            super.transformRegularClass(regularClass, data)
        }
    }

    private var lookupFunctions = false
    private var lookupProperties = false

    private inline fun <T> withNewSettings(block: () -> T): T {
        val prevFunctions = lookupFunctions
        val prevProperties = lookupProperties
        val result = block()

        lookupFunctions = prevFunctions
        lookupProperties = prevProperties
        return result
    }


    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Nothing?): CompositeTransformResult<FirStatement> {

        return withNewSettings {
            lookupFunctions = true
            lookupProperties = false
            super.transformFunctionCall(functionCall, data)
        }
    }


    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return withNewSettings {
            lookupProperties = true
            lookupFunctions = false
            super.transformQualifiedAccessExpression(qualifiedAccessExpression, data)
        }
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return withNewSettings {
            lookupProperties = true
            lookupFunctions = true
            super.transformCallableReferenceAccess(callableReferenceAccess, data)
        }
    }


    override fun transformAssignment(assignment: FirAssignment, data: Nothing?): CompositeTransformResult<FirStatement> {
        return withNewSettings {
            lookupProperties = true
            lookupFunctions = false
            super.transformAssignment(assignment, data)
        }
    }


    override fun transformNamedReference(namedReference: FirNamedReference, data: Nothing?): CompositeTransformResult<FirNamedReference> {
        if (namedReference is FirResolvedCallableReference) return namedReference.compose()
        val name = namedReference.name
        val referents = mutableListOf<ConeCallableSymbol>()
        fun collect(it: ConeCallableSymbol): ProcessorAction {
            referents.add(it)
            return NEXT
        }

        if (lookupFunctions)
            towerScope.processFunctionsByName(name, ::collect)
        if (lookupProperties)
            towerScope.processPropertiesByName(name, ::collect)

        return when (referents.size) {
            0 -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Unresolved name: $name"
            ).compose()
            1 -> FirResolvedCallableReferenceImpl(
                namedReference.session, namedReference.psi,
                name, referents.single()
            ).compose()
            else -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Ambiguity: $name, ${referents.map { it.callableId }}"
            ).compose()
        }

    }
}
