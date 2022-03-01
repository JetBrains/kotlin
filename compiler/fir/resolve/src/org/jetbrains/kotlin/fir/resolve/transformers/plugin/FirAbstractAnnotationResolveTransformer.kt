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

    inline fun <T> withFileScopes(file: FirFile, f: () -> T): T {
        scopes = createImportingScopes(file, session, scopeSession, useCaching = false)
        val state = beforeTransformingChildren(file)
        try {
            return f()
        } finally {
            afterTransformingChildren(state)
        }
    }

    override fun transformFile(file: FirFile, data: D): FirFile {
        withFileScopes(file) {
            scopes = createImportingScopes(file, session, scopeSession, useCaching = false)
            val state = beforeTransformingChildren(file)
            file.transformDeclarations(this, data)
            afterTransformingChildren(state)
        }
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
            val state = beforeTransformingChildren(regularClass)
            regularClass.transformDeclarations(this, data)
            regularClass.transformSuperTypeRefs(this, data)
            afterTransformingChildren(state)
        } as FirStatement
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: D
    ): FirSimpleFunction {
        return transformDeclaration(simpleFunction, data).also {
            val state = beforeTransformingChildren(simpleFunction)
            simpleFunction.transformValueParameters(this, data)
            afterTransformingChildren(state)
        } as FirSimpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: D
    ): FirConstructor {
        return transformDeclaration(constructor, data).also {
            val state = beforeTransformingChildren(constructor)
            constructor.transformValueParameters(this, data)
            afterTransformingChildren(state)
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

    /**
     * Gets called before transforming [parentDeclaration]'s nested declarations (like in a class of a file).
     *
     * @param parentDeclaration A declaration whose nested declarations are about to be transformed.
     * @return Some state of the transformer; when the nested declarations are transformed, this state will be
     * passed to the [afterTransformingChildren].
     */
    protected open fun beforeTransformingChildren(parentDeclaration: FirDeclaration): S? {
        return null
    }

    /**
     * Gets called after performing transformation of some declaration's nested declarations; can be used to restore the internal
     * state of the transformer.
     *
     * @param state A state produced by the [beforeTransformingChildren] call before the transformation.
     */
    protected open fun afterTransformingChildren(state: S?) {}
}
