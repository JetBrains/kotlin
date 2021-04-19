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
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.*

object DeclarationCopyBuilder {
    fun createDeclarationCopy(
        originalFirDeclaration: FirDeclaration,
        fakeKtDeclaration: KtDeclaration,
        state: FirModuleResolveState,
    ): FirDeclaration {
        return when (fakeKtDeclaration) {
            is KtNamedFunction -> createFunctionCopy(
                fakeKtDeclaration,
                originalFirDeclaration as FirSimpleFunction,
                state
            )
            is KtProperty -> createPropertyCopy(
                fakeKtDeclaration,
                originalFirDeclaration as FirProperty,
                state
            )
            is KtClassOrObject -> createClassCopy(
                fakeKtDeclaration,
                originalFirDeclaration as FirRegularClass,
                state
            )
            is KtTypeAlias -> createTypeAliasCopy(
                fakeKtDeclaration,
                originalFirDeclaration as FirTypeAlias,
                state
            )
            else -> error("Unsupported declaration ${fakeKtDeclaration::class.simpleName}")
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
            resolvePhase = minOf(originalFunction.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtFunction.source
            session = state.rootModuleSession
        }.apply { reassignAllReturnTargets(builtFunction) }
    }

    private fun createClassCopy(
        fakeKtClassOrObject: KtClassOrObject,
        originalFirClass: FirRegularClass,
        state: FirModuleResolveState,
    ): FirRegularClass {
        val builtClass = createCopy(fakeKtClassOrObject, originalFirClass)

        return buildRegularClassCopy(originalFirClass) {
            declarations.clear()
            declarations.addAll(builtClass.declarations)
            symbol = builtClass.symbol
            resolvePhase = minOf(originalFirClass.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtClass.source
            session = state.rootModuleSession
        }
    }

    private fun createTypeAliasCopy(
        fakeKtTypeAlias: KtTypeAlias,
        originalFirTypeAlias: FirTypeAlias,
        state: FirModuleResolveState,
    ): FirTypeAlias {
        val builtTypeAlias = createCopy(fakeKtTypeAlias, originalFirTypeAlias)

        return buildTypeAliasCopy(originalFirTypeAlias) {
            expandedTypeRef = builtTypeAlias.expandedTypeRef
            symbol = builtTypeAlias.symbol
            resolvePhase = minOf(originalFirTypeAlias.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtTypeAlias.source
            session = state.rootModuleSession
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
                resolvePhase = minOf(builtSetter.resolvePhase, FirResolvePhase.DECLARATIONS)
                source = builtSetter.source
                session = state.rootModuleSession
            }.apply { reassignAllReturnTargets(builtSetter) }
        } else {
            builtSetter
        }

        return buildPropertyCopy(originalProperty) {
            symbol = builtProperty.symbol
            initializer = builtProperty.initializer

            getter = builtProperty.getter
            setter = copySetter

            resolvePhase = minOf(originalProperty.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtProperty.source
            session = state.rootModuleSession
        }
    }

    internal inline fun <reified T : FirDeclaration> createCopy(
        fakeKtDeclaration: KtDeclaration,
        originalFirDeclaration: T,
    ): T {
        return RawFirFragmentForLazyBodiesBuilder.build(
            session = originalFirDeclaration.session,
            baseScopeProvider = originalFirDeclaration.session.firIdeProvider.kotlinScopeProvider,
            designation = originalFirDeclaration.collectDesignation().fullDesignation,
            declaration = fakeKtDeclaration
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