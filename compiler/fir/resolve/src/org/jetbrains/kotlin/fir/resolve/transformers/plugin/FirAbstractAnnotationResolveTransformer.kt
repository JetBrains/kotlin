/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.withScopeCleanup
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.compose

internal abstract class FirAbstractAnnotationResolveTransformer<D>(
    protected val session: FirSession,
    protected val scopeSession: ScopeSession
) : FirDefaultTransformer<D>() {
    abstract override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: D): CompositeTransformResult<FirStatement>

    protected val towerScope = FirCompositeScope(mutableListOf())

    override fun transformFile(file: FirFile, data: D): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(towerScope.scopes) {
            towerScope.addScopes(createImportingScopes(file, session, scopeSession))
            file.transformDeclarations(this, data)
            file.transformAnnotations(this, data).compose()
        }
    }

    override fun transformProperty(property: FirProperty, data: D): CompositeTransformResult<FirDeclaration> {
        return property.transformAnnotations(this, data).compose()
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: D
    ): CompositeTransformResult<FirStatement> {
        regularClass.transformDeclarations(this, data)
        regularClass.transformCompanionObject(this, data)
        regularClass.transformSuperTypeRefs(this, data)
        return regularClass.transformAnnotations(this, data).compose()
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: D
    ): CompositeTransformResult<FirDeclaration> {
        simpleFunction.transformValueParameters(this, data)
        return simpleFunction.transformAnnotations(this, data).compose()
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: D
    ): CompositeTransformResult<FirDeclaration> {
        constructor.transformValueParameters(this, data)
        return constructor.transformAnnotations(this, data).compose()
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: D
    ): CompositeTransformResult<FirStatement> {
        return valueParameter.transformAnnotations(this, data).compose()
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: D): CompositeTransformResult<FirDeclaration> {
        return typeAlias.transformAnnotations(this, data).compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return typeRef.transformAnnotations(this, data).compose()
    }

    override fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E> {
        return element.compose()
    }
}