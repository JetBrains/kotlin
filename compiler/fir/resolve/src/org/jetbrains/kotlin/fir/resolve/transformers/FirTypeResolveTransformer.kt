/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

open class FirTypeResolveTransformer : FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority = true) {
    private lateinit var session: FirSession

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        session = file.fileSession
        return withScopeCleanup {
            towerScope.addImportingScopes(file, session)
            super.transformFile(file, data)
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

            super.transformRegularClass(regularClass, data)
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            constructor.addTypeParametersScope()
            super.transformConstructor(constructor, data)
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            typeAlias.addTypeParametersScope()
            super.transformTypeAlias(typeAlias, data)
        }
    }


    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            property.addTypeParametersScope()
            super.transformProperty(property, data)
        }
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            namedFunction.addTypeParametersScope()
            super.transformNamedFunction(namedFunction, data)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        if (implicitTypeRef is FirImplicitBuiltinTypeRef) return super.transformImplicitTypeRef(implicitTypeRef, data)
        return implicitTypeRef.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return FirSpecificTypeResolverTransformer(towerScope, FirPosition.OTHER, session).transformTypeRef(typeRef, data)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirDeclaration> {

        val valueParameter = super.transformValueParameter(valueParameter, data).single as FirValueParameter

        if (valueParameter.isVararg) {
            val returnTypeRef = valueParameter.returnTypeRef
            val returnType = returnTypeRef.coneTypeUnsafe<ConeKotlinType>()
            valueParameter.transformReturnTypeRef(
                StoreType,
                valueParameter.returnTypeRef.withReplacedConeType(
                    session,
                    returnType.createArrayOf(session)
                )
            )
        }
        return valueParameter.compose()
    }

}
