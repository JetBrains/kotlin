/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

open class FirTypeResolveTransformer : FirAbstractTreeTransformerWithSuperTypes(
    phase = FirResolvePhase.TYPES,
    reversedScopePriority = true
) {
    override lateinit var session: FirSession

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        session = file.fileSession
        val scopeSession = ScopeSession()
        return withScopeCleanup {
            towerScope.addImportingScopes(file, session, scopeSession)
            transformElement(file, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        withScopeCleanup {
            regularClass.addTypeParametersScope()
            regularClass.typeParameters.forEach {
                it.accept(this, data)
            }
        }

        return withScopeCleanup {
            val session = session
            val firProvider = FirProvider.getInstance(session)
            val classId = regularClass.symbol.classId
            lookupSuperTypes(regularClass, lookupInterfaces = false, deep = true, useSiteSession = session)
                .asReversed().mapTo(towerScope.scopes) {
                    FirNestedClassifierScope(it.lookupTag.classId, FirSymbolProvider.getInstance(session))
                }
            val companionObjects = regularClass.declarations.filterIsInstance<FirRegularClass>().filter { it.isCompanion }
            for (companionObject in companionObjects) {
                towerScope.scopes += FirNestedClassifierScope(companionObject.symbol.classId, firProvider)
            }
            towerScope.scopes += FirNestedClassifierScope(classId, firProvider)
            regularClass.addTypeParametersScope()

            transformDeclaration(regularClass, data)
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            constructor.addTypeParametersScope()
            transformDeclaration(constructor, data)
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            typeAlias.addTypeParametersScope()
            transformDeclaration(typeAlias, data)
        }
    }


    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            property.addTypeParametersScope()
            transformDeclaration(property, data)
        }
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            namedFunction.addTypeParametersScope()
            transformDeclaration(namedFunction, data)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        if (implicitTypeRef is FirImplicitBuiltinTypeRef) return transformTypeRef(implicitTypeRef, data)
        return implicitTypeRef.compose()
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(resolvedTypeRef, data)
    }

    override fun transformErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(errorTypeRef, data)
    }

    override fun transformResolvedFunctionTypeRef(
        resolvedFunctionTypeRef: FirResolvedFunctionTypeRef,
        data: Nothing?
    ): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(resolvedFunctionTypeRef, data)
    }

    override fun transformTypeRefWithNullability(
        typeRefWithNullability: FirTypeRefWithNullability,
        data: Nothing?
    ): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(typeRefWithNullability, data)
    }

    override fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(userTypeRef, data)
    }

    override fun transformDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(dynamicTypeRef, data)
    }

    override fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(functionTypeRef, data)
    }

    override fun transformDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(delegatedTypeRef, data)
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return FirSpecificTypeResolverTransformer(towerScope, FirPosition.OTHER, session).transformTypeRef(typeRef, data)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val result = transformDeclaration(valueParameter, data).single as FirValueParameter
        if (result.isVararg) {
            val returnTypeRef = result.returnTypeRef
            val returnType = returnTypeRef.coneTypeUnsafe<ConeKotlinType>()
            result.transformReturnTypeRef(
                StoreType,
                result.returnTypeRef.withReplacedConeType(
                    returnType.createArrayOf(session)
                )
            )
        }
        return result.compose()
    }

}
