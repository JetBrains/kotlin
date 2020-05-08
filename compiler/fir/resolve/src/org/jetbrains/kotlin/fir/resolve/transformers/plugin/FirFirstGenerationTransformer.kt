/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.FirClassGenerationExtension
import org.jetbrains.kotlin.fir.extensions.classGenerationExtensions
import org.jetbrains.kotlin.fir.extensions.extensionsService
import org.jetbrains.kotlin.fir.extensions.hasExtensions
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.compose

class FirFirstGenerationTransformer : FirDefaultTransformer<Nothing?>() {
    private lateinit var session: FirSession
    private val generatedDeclarations: MutableList<FirClassGenerationExtension.GeneratedClass> = mutableListOf()
    private lateinit var file: FirFile
    private val extensionPointService get() = session.extensionsService

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        session = file.session
        if (!extensionPointService.hasExtensions) return file.compose()
        file.replaceResolvePhase(FirResolvePhase.FIRST_PLUGIN_GENERATION)
        generatedDeclarations.clear()
        this.file = file

        file.transformDeclarations(this, data)
        visitAnnotatedDeclaration(file)
        addGeneratedClasses()
        return file.compose()
    }

    private fun addGeneratedClasses() {
        for ((generatedClass, owner) in generatedDeclarations) {
            when (owner) {
                is FirFile -> owner.addDeclaration(generatedClass)
                is FirRegularClass -> owner.addDeclaration(generatedClass)
                else -> throw IllegalStateException()
            }
        }
    }

    private fun <T> visitAnnotatedDeclaration(declaration: T) where T : FirDeclaration, T : FirAnnotationContainer {
        val extensions = extensionPointService.classGenerationExtensions.forDeclaration(declaration, emptyList())
        extensions.flatMapTo(generatedDeclarations) { it.generateClass(file, declaration) }
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        visitAnnotatedDeclaration(property)
        return property.compose()
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        regularClass.transformDeclarations(this, data)
        regularClass.transformCompanionObject(this, data)
        visitAnnotatedDeclaration(regularClass)
        return regularClass.compose()
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        simpleFunction.transformValueParameters(this, data)
        visitAnnotatedDeclaration(simpleFunction)
        return simpleFunction.compose()
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        constructor.transformValueParameters(this, data)
        visitAnnotatedDeclaration(constructor)
        return constructor.compose()
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        visitAnnotatedDeclaration(valueParameter)
        return valueParameter.compose()
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        visitAnnotatedDeclaration(typeAlias)
        return typeAlias.compose()
    }

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }
}