/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.*

internal object FirLazyBodiesCalculator {
    fun calculateLazyBodiesInside(element: FirElement) {
        element.transform<FirElement, Nothing?>(FirLazyBodiesCalculatorTransformer, null)
    }

    fun calculateLazyBodiesIfPhaseRequires(firFile: FirFile, phase: FirResolvePhase) {
        if (phase == FIRST_PHASE_WHICH_NEEDS_BODIES) {
            calculateLazyBodiesInside(firFile)
        }
    }

    fun calculateLazyBodiesForFunction(simpleFunction: FirSimpleFunction) {
        if (simpleFunction.body !is FirLazyBlock) return
        val rawFirBuilder = createRawFirBuilder(simpleFunction)
        val newFunction = rawFirBuilder.buildFunctionWithBody(simpleFunction.psi as KtNamedFunction) as FirSimpleFunction
        simpleFunction.apply {
            replaceBody(newFunction.body)
            replaceContractDescription(newFunction.contractDescription)
        }
    }

    fun calculateLazyBodyForSecondaryConstructor(secondaryConstructor: FirConstructor) {
        require(!secondaryConstructor.isPrimary)
        if (secondaryConstructor.body !is FirLazyBlock) return
        val rawFirBuilder = createRawFirBuilder(secondaryConstructor)
        val newFunction = rawFirBuilder.buildSecondaryConstructor(secondaryConstructor.psi as KtSecondaryConstructor)
        secondaryConstructor.apply {
            replaceBody(newFunction.body)
        }
    }

    fun calculateLazyBodyForProperty(firProperty: FirProperty) {
        if (!needCalculatingLazyBodyForProperty(firProperty)) return

        val rawFirBuilder = createRawFirBuilder(firProperty)
        val newProperty = rawFirBuilder.buildPropertyWithBody(firProperty.psi as KtProperty)
        newProperty.apply {
            getter?.takeIf { it.body is FirLazyBlock }?.let { getter ->
                val newGetter = newProperty.getter!!
                getter.replaceBody(newGetter.body)
                getter.replaceContractDescription(newGetter.contractDescription)
            }
            setter?.takeIf { it.body is FirLazyBlock }?.let { setter ->
                val newSetter = newProperty.setter!!
                setter.replaceBody(newSetter.body)
                setter.replaceContractDescription(newSetter.contractDescription)
            }
            if (firProperty.initializer is FirLazyExpression) {
                firProperty.replaceInitializer(newProperty.initializer)
            }
        }
    }

    fun needCalculatingLazyBodyForProperty(firProperty: FirProperty): Boolean =
        firProperty.getter?.body is FirLazyBlock
                || firProperty.setter?.body is FirLazyBlock
                || firProperty.initializer is FirLazyExpression


    private fun createRawFirBuilder(firDeclaration: FirDeclaration): RawFirBuilder {
        val scopeProvider = firDeclaration.session.firIdeProvider.kotlinScopeProvider
        return RawFirBuilder(firDeclaration.session, scopeProvider)
    }

    private val FIRST_PHASE_WHICH_NEEDS_BODIES = FirResolvePhase.CONTRACTS
}

private object FirLazyBodiesCalculatorTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (simpleFunction.body is FirLazyBlock) {
            FirLazyBodiesCalculator.calculateLazyBodiesForFunction(simpleFunction)
            return simpleFunction.compose()
        }
        return (simpleFunction.transformChildren(this, data) as FirDeclaration).compose()
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (constructor.body is FirLazyBlock) {
            FirLazyBodiesCalculator.calculateLazyBodyForSecondaryConstructor(constructor)
            return constructor.compose()
        }
        return (constructor.transformChildren(this, data) as FirDeclaration).compose()
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (FirLazyBodiesCalculator.needCalculatingLazyBodyForProperty(property)) {
            FirLazyBodiesCalculator.calculateLazyBodyForProperty(property)
            return property.compose()
        }
        return super.transformProperty(property, data)
    }
}