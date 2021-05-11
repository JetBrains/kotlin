/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

internal object DeclarationCopyBuilder {
    fun FirSimpleFunction.withBodyFrom(
        functionWithBody: FirSimpleFunction,
    ): FirSimpleFunction = buildSimpleFunctionCopy(this) {
        body = functionWithBody.body
        symbol = functionWithBody.symbol
        initDeclaration(this@withBodyFrom, functionWithBody)
    }.apply { reassignAllReturnTargets(functionWithBody) }

    fun FirRegularClass.withBodyFrom(
        classWithBody: FirRegularClass,
    ): FirRegularClass = buildRegularClassCopy(this) {
        declarations.clear()
        declarations.addAll(classWithBody.declarations)
        symbol = classWithBody.symbol
        initDeclaration(this@withBodyFrom, classWithBody)
        resolvePhase = minOf(this.resolvePhase, FirResolvePhase.IMPORTS) //TODO move into initDeclaration?
    }

    fun FirProperty.withBodyFrom(propertyWithBody: FirProperty): FirProperty {
        val originalSetter = this@withBodyFrom.setter
        val replacementSetter = propertyWithBody.setter

        // setter has a header with `value` parameter, and we want it type to be resolved
        val copySetter = if (originalSetter != null && replacementSetter != null) {
            buildPropertyAccessorCopy(originalSetter) {
                body = replacementSetter.body
                symbol = replacementSetter.symbol
                initDeclaration(originalSetter, replacementSetter)
            }.apply { reassignAllReturnTargets(replacementSetter) }
        } else {
            replacementSetter
        }

        val propertyResolvePhase = minOf(
            this@withBodyFrom.resolvePhase,
            FirResolvePhase.DECLARATIONS,
            copySetter?.resolvePhase ?: FirResolvePhase.BODY_RESOLVE,
            propertyWithBody.getter?.resolvePhase ?: FirResolvePhase.BODY_RESOLVE,
        )

        return buildPropertyCopy(this@withBodyFrom) {
            symbol = propertyWithBody.symbol
            initializer = propertyWithBody.initializer

            getter = propertyWithBody.getter
            setter = copySetter

            initDeclaration(this@withBodyFrom, propertyWithBody)
            resolvePhase = propertyResolvePhase
        }
    }

    private fun FirDeclarationBuilder.initDeclaration(
        originalDeclaration: FirDeclaration,
        builtDeclaration: FirDeclaration,
    ) {
        resolvePhase = minOf(originalDeclaration.resolvePhase, FirResolvePhase.DECLARATIONS)
        source = builtDeclaration.source
        moduleData = originalDeclaration.moduleData
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
