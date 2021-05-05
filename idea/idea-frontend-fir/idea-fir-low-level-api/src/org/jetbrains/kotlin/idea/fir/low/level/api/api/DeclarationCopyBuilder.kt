/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirFragmentForLazyBodiesBuilder
import org.jetbrains.kotlin.fir.builder.RawFirReplacement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getContainingFile
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*

object DeclarationCopyBuilder {
    fun <T : KtElement> createDeclarationCopy(
        state: FirModuleResolveState,
        nonLocalDeclaration: KtDeclaration,
        replacement: RawFirReplacement<T>
    ): FirDeclaration {

        return when (nonLocalDeclaration) {
            is KtNamedFunction -> createFunctionCopy(
                nonLocalDeclaration,
                state,
                replacement
            )
            is KtProperty -> createPropertyCopy(
                nonLocalDeclaration,
                state,
                replacement
            )
            is KtClassOrObject -> createClassCopy(
                nonLocalDeclaration,
                state,
                replacement
            )
            is KtTypeAlias -> createTypeAliasCopy(
                nonLocalDeclaration,
                state,
                replacement
            )
            else -> error("Unsupported declaration ${nonLocalDeclaration::class.simpleName}")
        }
    }

    private fun <T : KtElement> createFunctionCopy(
        rootNonLocalDeclaration: KtNamedFunction,
        state: FirModuleResolveState,
        replacement: RawFirReplacement<T>,
    ): FirSimpleFunction {

        val originalFunction = rootNonLocalDeclaration.getOrBuildFirOfType<FirSimpleFunction>(state)
        val builtFunction = createCopy(rootNonLocalDeclaration, originalFunction, replacement)

        // right now we can't resolve builtFunction header properly, as it built right in air,
        // without file, which is now required for running stages other then body resolve, so we
        // take original function header (which is resolved) and copy replacing body with body from builtFunction
        return buildSimpleFunctionCopy(originalFunction) {
            body = builtFunction.body
            symbol = builtFunction.symbol
            initDeclaration(originalFunction, builtFunction, state)
        }.apply { reassignAllReturnTargets(builtFunction) }
    }

    private fun <T : KtElement> createClassCopy(
        rootNonLocalDeclaration: KtClassOrObject,
        state: FirModuleResolveState,
        replacement: RawFirReplacement<T>,
    ): FirRegularClass {
        val originalFirClass = rootNonLocalDeclaration.getOrBuildFirOfType<FirRegularClass>(state)
        val builtClass = createCopy(rootNonLocalDeclaration, originalFirClass, replacement)

        return buildRegularClassCopy(originalFirClass) {
            declarations.clear()
            declarations.addAll(builtClass.declarations)
            symbol = builtClass.symbol
            initDeclaration(originalFirClass, builtClass, state)
            resolvePhase = minOf(originalFirClass.resolvePhase, FirResolvePhase.IMPORTS) //TODO move into initDeclaration?
        }
    }

    private fun <T : KtElement> createTypeAliasCopy(
        rootNonLocalDeclaration: KtTypeAlias,
        state: FirModuleResolveState,
        replacement: RawFirReplacement<T>,
    ): FirTypeAlias {

        val originalFirTypeAlias = rootNonLocalDeclaration.getOrBuildFirOfType<FirTypeAlias>(state)
        val builtTypeAlias = createCopy(rootNonLocalDeclaration, originalFirTypeAlias, replacement)

        return buildTypeAliasCopy(originalFirTypeAlias) {
            expandedTypeRef = builtTypeAlias.expandedTypeRef
            symbol = builtTypeAlias.symbol
            initDeclaration(originalFirTypeAlias, builtTypeAlias, state)
        }
    }

    private fun <T : KtElement> createPropertyCopy(
        rootNonLocalDeclaration: KtProperty,
        state: FirModuleResolveState,
        replacement: RawFirReplacement<T>,
    ): FirProperty {
        val originalProperty = rootNonLocalDeclaration.getOrBuildFirOfType<FirProperty>(state)
        val builtProperty = createCopy(rootNonLocalDeclaration, originalProperty, replacement)

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

    internal inline fun <reified D : FirDeclaration, T : KtElement> createCopy(
        rootNonLocalDeclaration: KtDeclaration,
        originalFirDeclaration: D,
        replacement: RawFirReplacement<T>? = null,
    ): D {
        return RawFirFragmentForLazyBodiesBuilder.buildWithReplacement(
            session = originalFirDeclaration.moduleData.session,
            baseScopeProvider = originalFirDeclaration.moduleData.session.firIdeProvider.kotlinScopeProvider,
            designation = originalFirDeclaration.collectDesignation().path,
            declarationToBuild = rootNonLocalDeclaration,
            replacement = replacement,
        ) as D
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
