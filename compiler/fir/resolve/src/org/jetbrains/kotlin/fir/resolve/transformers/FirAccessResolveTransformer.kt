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
import org.jetbrains.kotlin.fir.resolve.buildUseSiteScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.impl.FirTopLevelDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirAccessResolveTransformer : FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority = true) {

    private lateinit var session: FirSession

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        session = file.fileSession
        return withScopeCleanup {
            towerScope.scopes += FirTopLevelDeclaredMemberScope(file, session)
            super.transformFile(file, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            towerScope.scopes += regularClass.buildUseSiteScope(session)
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
                session, namedReference.psi, "Unresolved name: $name"
            ).compose()
            1 -> FirResolvedCallableReferenceImpl(
                session, namedReference.psi,
                name, referents.single()
            ).compose()
            else -> FirErrorNamedReference(
                session, namedReference.psi, "Ambiguity: $name, ${referents.map { it.callableId }}"
            ).compose()
        }

    }
}
