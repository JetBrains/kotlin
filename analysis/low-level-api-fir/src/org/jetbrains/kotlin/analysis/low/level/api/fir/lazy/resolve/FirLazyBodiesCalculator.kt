/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignation
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

internal object FirLazyBodiesCalculator {
    fun calculateLazyBodiesInside(designation: FirDeclarationDesignation) {
        designation.declaration.transform<FirElement, PersistentList<FirDeclaration>>(
            FirLazyBodiesCalculatorTransformer,
            designation.toSequence(includeTarget = false).toList().toPersistentList()
        )
    }

    fun calculateLazyBodies(firFile: FirFile) {
        firFile.transform<FirElement, PersistentList<FirDeclaration>>(FirLazyBodiesCalculatorTransformer, persistentListOf())
    }

    fun calculateLazyBodiesForFunction(designation: FirDeclarationDesignation) {
        val simpleFunction = designation.declaration as FirSimpleFunction
        if (simpleFunction.body !is FirLazyBlock) return
        val newFunction = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = simpleFunction.moduleData.session,
            scopeProvider = simpleFunction.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = simpleFunction.psi as KtNamedFunction,
        ) as FirSimpleFunction
        simpleFunction.apply {
            replaceBody(newFunction.body)
            replaceContractDescription(newFunction.contractDescription)
        }
    }

    fun calculateLazyBodyForSecondaryConstructor(designation: FirDeclarationDesignation) {
        val secondaryConstructor = designation.declaration as FirConstructor
        require(!secondaryConstructor.isPrimary)
        if (secondaryConstructor.body !is FirLazyBlock) return

        val newFunction = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = secondaryConstructor.moduleData.session,
            scopeProvider = secondaryConstructor.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = secondaryConstructor.psi as KtSecondaryConstructor,
        ) as FirSimpleFunction

        secondaryConstructor.apply {
            replaceBody(newFunction.body)
        }
    }

    fun calculateLazyBodyForProperty(designation: FirDeclarationDesignation) {
        val firProperty = designation.declaration as FirProperty
        if (!needCalculatingLazyBodyForProperty(firProperty)) return

        val newProperty = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = firProperty.moduleData.session,
            scopeProvider = firProperty.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = firProperty.psi as KtProperty
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

        firProperty.getExplicitBackingField()?.takeIf { it.initializer is FirLazyExpression }?.let { backingField ->
            val newInitializer = newProperty.getExplicitBackingField()?.initializer
            backingField.replaceInitializer(newInitializer)
        }

        val delegate = firProperty.delegate as? FirWrappedDelegateExpression
        val delegateExpression = delegate?.expression
        if (delegateExpression is FirLazyExpression) {
            val newDelegate = newProperty.delegate as? FirWrappedDelegateExpression
            check(newDelegate != null) { "Invalid replacement delegate" }
            delegate.replaceExpression(newDelegate.expression)

            val delegateProviderCall = delegate.delegateProvider as? FirFunctionCall
            val delegateProviderExplicitReceiver = delegateProviderCall?.explicitReceiver
            if (delegateProviderExplicitReceiver is FirLazyExpression) {
                val newDelegateProviderExplicitReceiver = (newDelegate.delegateProvider as? FirFunctionCall)?.explicitReceiver
                check(newDelegateProviderExplicitReceiver != null) { "Invalid replacement expression" }
                delegateProviderCall.replaceExplicitReceiver(newDelegateProviderExplicitReceiver)
            }
        }
    }

    fun needCalculatingLazyBodyForProperty(firProperty: FirProperty): Boolean =
        firProperty.getter?.body is FirLazyBlock
                || firProperty.setter?.body is FirLazyBlock
                || firProperty.initializer is FirLazyExpression
                || (firProperty.delegate as? FirWrappedDelegateExpression)?.expression is FirLazyExpression
                || firProperty.getExplicitBackingField()?.initializer is FirLazyExpression
}

private object FirLazyBodiesCalculatorTransformer : FirTransformer<PersistentList<FirDeclaration>>() {

    override fun transformFile(file: FirFile, data: PersistentList<FirDeclaration>): FirFile {
        file.declarations.forEach {
            it.transformSingle(this, data)
        }
        return file
    }

    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirDeclaration>): E {
        if (element is FirRegularClass) {
            val newList = data.add(element)
            element.declarations.forEach {
                it.transformSingle(this, newList)
            }
            element.transformChildren(this, newList)
        }
        return element
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: PersistentList<FirDeclaration>
    ): FirSimpleFunction {
        if (simpleFunction.body is FirLazyBlock) {
            val designation = FirDeclarationDesignation(data, simpleFunction)
            FirLazyBodiesCalculator.calculateLazyBodiesForFunction(designation)
        }
        return simpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: PersistentList<FirDeclaration>
    ): FirConstructor {
        if (constructor.body is FirLazyBlock) {
            val designation = FirDeclarationDesignation(data, constructor)
            FirLazyBodiesCalculator.calculateLazyBodyForSecondaryConstructor(designation)
        }
        return constructor
    }

    override fun transformProperty(property: FirProperty, data: PersistentList<FirDeclaration>): FirProperty {
        if (FirLazyBodiesCalculator.needCalculatingLazyBodyForProperty(property)) {
            val designation = FirDeclarationDesignation(data, property)
            FirLazyBodiesCalculator.calculateLazyBodyForProperty(designation)
        }
        return property
    }
}
