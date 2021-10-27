/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer

internal abstract class FirAbstractAnnotationResolveTransformer<D, S>(
    protected val session: FirSession,
    protected val scopeSession: ScopeSession
) : FirDefaultTransformer<D>() {
    abstract override fun transformAnnotation(annotation: FirAnnotation, data: D): FirStatement

    protected lateinit var scopes: List<FirScope>

    override fun transformFile(file: FirFile, data: D): FirFile {
        scopes = createImportingScopes(file, session, scopeSession, useCaching = false)
        val state = beforeChildren(file)
        file.transformDeclarations(this, data)
        afterChildren(state)
        return transformDeclaration(file, data) as FirFile
    }

    override fun transformProperty(property: FirProperty, data: D): FirProperty {
        return transformDeclaration(property, data) as FirProperty
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: D
    ): FirStatement {
        return transformDeclaration(regularClass, data).also {
            val state = beforeChildren(regularClass)
            regularClass.transformDeclarations(this, data)
            regularClass.transformCompanionObject(this, data)
            regularClass.transformSuperTypeRefs(this, data)
            afterChildren(state)
        } as FirStatement
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: D
    ): FirSimpleFunction {
        return transformDeclaration(simpleFunction, data).also {
            val state = beforeChildren(simpleFunction)
            simpleFunction.transformValueParameters(this, data)
            afterChildren(state)
        } as FirSimpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: D
    ): FirConstructor {
        return transformDeclaration(constructor, data).also {
            val state = beforeChildren(constructor)
            constructor.transformValueParameters(this, data)
            afterChildren(state)
        } as FirConstructor
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: D
    ): FirStatement {
        return transformDeclaration(valueParameter, data) as FirStatement
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: D): FirTypeAlias {
        return transformDeclaration(typeAlias, data) as FirTypeAlias
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: D): FirTypeRef {
        return transformAnnotationContainer(typeRef, data) as FirTypeRef
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: D): FirDeclaration {
        return transformAnnotationContainer(declaration, data) as FirDeclaration
    }

    override fun transformAnnotationContainer(
        annotationContainer: FirAnnotationContainer,
        data: D
    ): FirAnnotationContainer {
        return annotationContainer.transformAnnotations(this, data)
    }

    override fun <E : FirElement> transformElement(element: E, data: D): E {
        return element
    }

    protected open fun beforeChildren(declaration: FirDeclaration): S? {
        return null
    }

    protected open fun afterChildren(state: S?) {}
}
