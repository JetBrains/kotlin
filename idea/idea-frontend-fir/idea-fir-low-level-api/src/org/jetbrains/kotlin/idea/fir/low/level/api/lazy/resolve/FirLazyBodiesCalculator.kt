/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirFragmentForLazyBodiesBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.*

internal object FirLazyBodiesCalculator {
    fun calculateLazyBodiesInside(element: FirElement, designation: List<FirDeclaration>) {
        element.transform<FirElement, MutableList<FirDeclaration>>(FirLazyBodiesCalculatorTransformer, designation.toMutableList())
    }

    fun calculateLazyBodiesIfPhaseRequires(firFile: FirFile, phase: FirResolvePhase) {
        if (phase == FIRST_PHASE_WHICH_NEEDS_BODIES) {
            firFile.transform<FirElement, MutableList<FirDeclaration>>(FirLazyBodiesCalculatorTransformer, mutableListOf())
        }
    }

    fun calculateLazyBodiesForFunction(simpleFunction: FirSimpleFunction, designation: List<FirDeclaration>) {
        if (simpleFunction.body !is FirLazyBlock) return
        val newFunction = RawFirFragmentForLazyBodiesBuilder.build(
            session = simpleFunction.session,
            baseScopeProvider = simpleFunction.session.firIdeProvider.kotlinScopeProvider,
            designation = designation,
            declaration = simpleFunction.psi as KtNamedFunction
        ) as FirSimpleFunction
        simpleFunction.apply {
            replaceBody(newFunction.body)
            replaceContractDescription(newFunction.contractDescription)
        }
    }

    fun calculateLazyBodyForSecondaryConstructor(secondaryConstructor: FirConstructor, designation: List<FirDeclaration>) {
        require(!secondaryConstructor.isPrimary)
        if (secondaryConstructor.body !is FirLazyBlock) return

        val newFunction = RawFirFragmentForLazyBodiesBuilder.build(
            session = secondaryConstructor.session,
            baseScopeProvider = secondaryConstructor.session.firIdeProvider.kotlinScopeProvider,
            designation = designation,
            declaration = secondaryConstructor.psi as KtSecondaryConstructor
        ) as FirSimpleFunction

        secondaryConstructor.apply {
            replaceBody(newFunction.body)
        }
    }

    fun calculateLazyBodyForProperty(firProperty: FirProperty, designation: List<FirDeclaration>) {
        if (!needCalculatingLazyBodyForProperty(firProperty)) return

        val newProperty = RawFirFragmentForLazyBodiesBuilder.build(
            session = firProperty.session,
            baseScopeProvider = firProperty.session.firIdeProvider.kotlinScopeProvider,
            designation = designation,
            declaration = firProperty.psi as KtProperty
        ) as FirProperty

        firProperty.getter?.takeIf { it.body is FirLazyBlock }?.let { getter ->
            val newGetter = newProperty.getter!!
            getter.replaceBody(newGetter.body)
            getter.replaceContractDescription(newGetter.contractDescription)
        }

        firProperty.setter?.takeIf { it.body is FirLazyBlock }?.let { setter ->
            val newSetter = newProperty.setter!!
            setter.replaceBody(newSetter.body)
            setter.replaceContractDescription(newSetter.contractDescription)
        }

        if (firProperty.initializer is FirLazyExpression) {
            firProperty.replaceInitializer(newProperty.initializer)
        }

        val delegate = firProperty.delegate
        if (delegate is FirWrappedDelegateExpression && delegate.expression is FirLazyExpression) {
            val newDelegate = newProperty.delegate as FirWrappedDelegateExpression
            delegate.replaceExpression(newDelegate.expression)
        }
    }

    fun needCalculatingLazyBodyForProperty(firProperty: FirProperty): Boolean =
        firProperty.getter?.body is FirLazyBlock
                || firProperty.setter?.body is FirLazyBlock
                || firProperty.initializer is FirLazyExpression
                || (firProperty.delegate as? FirWrappedDelegateExpression)?.expression is FirLazyExpression

    private val FIRST_PHASE_WHICH_NEEDS_BODIES = FirResolvePhase.CONTRACTS
}

private object FirLazyBodiesCalculatorTransformer : FirTransformer<MutableList<FirDeclaration>>() {

    override fun transformFile(file: FirFile, data: MutableList<FirDeclaration>): CompositeTransformResult<FirDeclaration> {
        file.declarations.forEach {
            it.transformSingle(this, data)
        }
        return file.compose()
    }

    override fun <E : FirElement> transformElement(element: E, data: MutableList<FirDeclaration>): CompositeTransformResult<E> {
        if (element is FirRegularClass) {
            data.add(element)
            element.declarations.forEach {
                it.transformSingle(this, data)
            }
            element.transformChildren(this, data)
            data.removeLast()
        }
        return element.compose()
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: MutableList<FirDeclaration>
    ): CompositeTransformResult<FirDeclaration> {
        if (simpleFunction.body is FirLazyBlock) {
            FirLazyBodiesCalculator.calculateLazyBodiesForFunction(simpleFunction, data)
        }
        return simpleFunction.compose()
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: MutableList<FirDeclaration>
    ): CompositeTransformResult<FirDeclaration> {
        if (constructor.body is FirLazyBlock) {
            FirLazyBodiesCalculator.calculateLazyBodyForSecondaryConstructor(constructor, data)
        }
        return constructor.compose()
    }

    override fun transformProperty(property: FirProperty, data: MutableList<FirDeclaration>): CompositeTransformResult<FirDeclaration> {
        if (FirLazyBodiesCalculator.needCalculatingLazyBodyForProperty(property)) {
            FirLazyBodiesCalculator.calculateLazyBodyForProperty(property, data)
        }
        return property.compose()
    }
}
