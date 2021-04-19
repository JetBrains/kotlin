/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirFragmentForLazyBodiesBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessorCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

object DeclarationCopyBuilder {
    fun createDeclarationCopy(
        originalFirDeclaration: FirDeclaration,
        fakeKtDeclaration: KtDeclaration,
        state: FirModuleResolveState,
    ): FirDeclaration {
        return when (fakeKtDeclaration) {
            is KtNamedFunction -> buildFunctionCopy(
                fakeKtDeclaration,
                originalFirDeclaration as FirSimpleFunction,
                state
            )
            is KtProperty -> buildPropertyCopy(
                fakeKtDeclaration,
                originalFirDeclaration as FirProperty,
                state
            )
            else -> error("Unsupported declaration ${fakeKtDeclaration::class.simpleName}")
        }
    }

    private fun buildFunctionCopy(
        element: KtNamedFunction,
        originalFunction: FirSimpleFunction,
        state: FirModuleResolveState,
    ): FirSimpleFunction {
        val builtFunction = createCopy(element, originalFunction) as FirSimpleFunction

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

    private fun buildPropertyCopy(
        element: KtProperty,
        originalProperty: FirProperty,
        state: FirModuleResolveState
    ): FirProperty {
        val builtProperty = createCopy(element, originalProperty) as FirProperty

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

        return org.jetbrains.kotlin.fir.declarations.builder.buildPropertyCopy(originalProperty) {
            symbol = builtProperty.symbol
            initializer = builtProperty.initializer

            getter = builtProperty.getter
            setter = copySetter

            resolvePhase = minOf(originalProperty.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtProperty.source
            session = state.rootModuleSession
        }
    }

    fun createCopy(
        fakeKtDeclaration: KtDeclaration,
        originalFirDeclaration: FirDeclaration,
    ): FirDeclaration {
        return RawFirFragmentForLazyBodiesBuilder.build(
            session = originalFirDeclaration.session,
            baseScopeProvider = originalFirDeclaration.session.firIdeProvider.kotlinScopeProvider,
            designation = originalFirDeclaration.collectDesignation().fullDesignation,
            declaration = fakeKtDeclaration
        )
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