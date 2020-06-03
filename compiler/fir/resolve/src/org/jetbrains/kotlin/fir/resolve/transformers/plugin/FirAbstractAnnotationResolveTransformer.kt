/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirAnnotationContainer
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

internal abstract class FirAbstractAnnotationResolveTransformer<D, S>(
    protected val session: FirSession,
    protected val scopeSession: ScopeSession
) : FirDefaultTransformer<D>() {
    abstract override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: D): CompositeTransformResult<FirStatement>

    protected val towerScope = FirCompositeScope(mutableListOf())

    override fun transformFile(file: FirFile, data: D): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(towerScope.scopes) {
            towerScope.addScopes(createImportingScopes(file, session, scopeSession, useCaching = false))
            val state = beforeChildren(file)
            file.transformDeclarations(this, data)
            afterChildren(state)
            transformAnnotatedDeclaration(file, data)
        }
    }

    override fun transformProperty(property: FirProperty, data: D): CompositeTransformResult<FirDeclaration> {
        return transformAnnotatedDeclaration(property, data)
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: D
    ): CompositeTransformResult<FirStatement> {
        @Suppress("UNCHECKED_CAST")
        return transformAnnotatedDeclaration(regularClass, data).also {
            val state = beforeChildren(regularClass)
            regularClass.transformDeclarations(this, data)
            regularClass.transformCompanionObject(this, data)
            regularClass.transformSuperTypeRefs(this, data)
            afterChildren(state)
        } as CompositeTransformResult<FirStatement>
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: D
    ): CompositeTransformResult<FirDeclaration> {
        return transformAnnotatedDeclaration(simpleFunction, data).also {
            val state = beforeChildren(simpleFunction)
            simpleFunction.transformValueParameters(this, data)
            afterChildren(state)
        }
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: D
    ): CompositeTransformResult<FirDeclaration> {
        return transformAnnotatedDeclaration(constructor, data).also {
            val state = beforeChildren(constructor)
            constructor.transformValueParameters(this, data)
            afterChildren(state)
        }
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: D
    ): CompositeTransformResult<FirStatement> {
        @Suppress("UNCHECKED_CAST")
        return transformAnnotatedDeclaration(valueParameter, data) as CompositeTransformResult<FirStatement>
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: D): CompositeTransformResult<FirDeclaration> {
        return transformAnnotatedDeclaration(typeAlias, data)
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        @Suppress("UNCHECKED_CAST")
        return transformAnnotationContainer(typeRef, data) as CompositeTransformResult<FirTypeRef>
    }

    override fun transformAnnotatedDeclaration(
        annotatedDeclaration: FirAnnotatedDeclaration,
        data: D
    ): CompositeTransformResult<FirDeclaration> {
        @Suppress("UNCHECKED_CAST")
        return transformAnnotationContainer(annotatedDeclaration, data) as CompositeTransformResult<FirDeclaration>
    }

    override fun transformAnnotationContainer(
        annotationContainer: FirAnnotationContainer,
        data: D
    ): CompositeTransformResult<FirAnnotationContainer> {
        return annotationContainer.transformAnnotations(this, data).compose()
    }

    override fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E> {
        return element.compose()
    }

    protected open fun beforeChildren(declaration: FirAnnotatedDeclaration): S? {
        return null
    }

    protected open fun afterChildren(state: S?) {}
}