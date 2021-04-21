/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirFragmentForLazyBodiesBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.*

object DeclarationCopyBuilder {
    fun createDeclarationCopy(
        originalFirDeclaration: FirDeclaration,
        copiedKtDeclaration: KtDeclaration,
        state: FirModuleResolveState,
    ): FirDeclaration {
        return when (copiedKtDeclaration) {
            is KtNamedFunction -> createFunctionCopy(
                copiedKtDeclaration,
                originalFirDeclaration as FirSimpleFunction,
                state
            )
            is KtProperty -> createPropertyCopy(
                copiedKtDeclaration,
                originalFirDeclaration as FirProperty,
                state
            )
            is KtClassOrObject -> createClassCopy(
                copiedKtDeclaration,
                originalFirDeclaration as FirRegularClass,
                state
            )
            is KtTypeAlias -> createTypeAliasCopy(
                copiedKtDeclaration,
                originalFirDeclaration as FirTypeAlias,
                state
            )
            else -> error("Unsupported declaration ${copiedKtDeclaration::class.simpleName}")
        }
    }

    private fun createFunctionCopy(
        element: KtNamedFunction,
        originalFunction: FirSimpleFunction,
        state: FirModuleResolveState,
    ): FirSimpleFunction {
        val builtFunction = createCopy(element, originalFunction)

        // right now we can't resolve builtFunction header properly, as it built right in air,
        // without file, which is now required for running stages other then body resolve, so we
        // take original function header (which is resolved) and copy replacing body with body from builtFunction
        return buildSimpleFunctionCopy(originalFunction) {
            body = builtFunction.body
            symbol = builtFunction.symbol
            initDeclaration(originalFunction, builtFunction, state)
        }.apply { reassignAllReturnTargets(builtFunction) }
    }

    private fun createClassCopy(
        copiedKtClassOrObject: KtClassOrObject,
        originalFirClass: FirRegularClass,
        state: FirModuleResolveState,
    ): FirRegularClass {
        val builtClass = createCopy(copiedKtClassOrObject, originalFirClass)

        return buildRegularClassCopy(originalFirClass) {
            declarations.clear()
            declarations.addAll(builtClass.declarations)
            symbol = builtClass.symbol
            initDeclaration(originalFirClass, builtClass, state)
        }
    }

    private fun createTypeAliasCopy(
        copiedKtTypeAlias: KtTypeAlias,
        originalFirTypeAlias: FirTypeAlias,
        state: FirModuleResolveState,
    ): FirTypeAlias {
        val builtTypeAlias = createCopy(copiedKtTypeAlias, originalFirTypeAlias)

        return buildTypeAliasCopy(originalFirTypeAlias) {
            expandedTypeRef = builtTypeAlias.expandedTypeRef
            symbol = builtTypeAlias.symbol
            initDeclaration(originalFirTypeAlias, builtTypeAlias, state)
        }
    }

    private fun createPropertyCopy(
        element: KtProperty,
        originalProperty: FirProperty,
        state: FirModuleResolveState
    ): FirProperty {
        val builtProperty = createCopy(element, originalProperty)

        val originalSetter = originalProperty.setter
        val builtSetter = builtProperty.setter

        // setter has a header with `value` parameter, and we want it type to be resolved
        val copySetter = if (originalSetter != null && builtSetter != null) {
            buildPropertyAccessorCopy(originalSetter) {
                body = builtSetter.body
                symbol = builtSetter.symbol
                initDeclaration(originalSetter, builtSetter, state)
            }.apply { reassignAllReturnTargets(builtSetter) }
        } else {
            builtSetter
        }

        return buildPropertyCopy(originalProperty) {
            symbol = builtProperty.symbol
            initializer = builtProperty.initializer

            getter = builtProperty.getter
            setter = copySetter

            initDeclaration(originalProperty, builtProperty, state)
        }
    }

    private fun FirDeclarationBuilder.initDeclaration(
        originalDeclaration: FirDeclaration,
        builtDeclaration: FirDeclaration,
        state: FirModuleResolveState
    ) {
        resolvePhase = minOf(originalDeclaration.resolvePhase, FirResolvePhase.DECLARATIONS)
        source = builtDeclaration.source
        moduleData = state.rootModuleSession.moduleData
    }

    internal inline fun <reified T : FirDeclaration> createCopy(
        copiedKtDeclaration: KtDeclaration,
        originalFirDeclaration: T,
    ): T {
        return RawFirFragmentForLazyBodiesBuilder.build(
            session = originalFirDeclaration.moduleData.session,
            baseScopeProvider = originalFirDeclaration.moduleData.session.firIdeProvider.kotlinScopeProvider,
            designation = originalFirDeclaration.collectDesignation().fullDesignation,
            declaration = copiedKtDeclaration
        ) as T
    }

    private fun FirFunction<*>.reassignAllReturnTargets(from: FirFunction<*>) {
        this.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirReturnExpression && element.target.labeledElement == from) {
                    element.target.bind(this@reassignAllReturnTargets)
                }
                element.acceptChildren(this)
            }
        })
    }
}
