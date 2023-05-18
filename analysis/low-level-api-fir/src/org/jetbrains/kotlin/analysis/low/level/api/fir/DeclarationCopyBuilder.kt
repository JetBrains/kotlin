/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

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
    }

    fun FirScript.withBodyFrom(
        scriptWithBody: FirScript
    ): FirScript = buildScriptCopy(this) {
        statements.clear()
        statements.addAll(scriptWithBody.statements)
        symbol = scriptWithBody.symbol
        initDeclaration(this@withBodyFrom, scriptWithBody)
    }

    fun FirProperty.withBodyFrom(propertyWithBody: FirProperty): FirProperty {
        val newSetter = getAccessorToUse(this, propertyWithBody) { it.setter }
        val newGetter = getAccessorToUse(this, propertyWithBody) { it.getter }

        val propertyResolvePhase = minOf(
            this@withBodyFrom.resolvePhase,
            FirResolvePhase.DECLARATIONS,
            newGetter?.resolvePhase ?: FirResolvePhase.BODY_RESOLVE,
            newSetter?.resolvePhase ?: FirResolvePhase.BODY_RESOLVE,
            propertyWithBody.getter?.resolvePhase ?: FirResolvePhase.BODY_RESOLVE,
            propertyWithBody.setter?.resolvePhase ?: FirResolvePhase.BODY_RESOLVE,
        )

        return buildPropertyCopy(this@withBodyFrom) {
            symbol = propertyWithBody.symbol
            initializer = propertyWithBody.initializer

            getter = newGetter
            setter = newSetter

            if (propertyResolvePhase < FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
                bodyResolveState = FirPropertyBodyResolveState.NOTHING_RESOLVED
            }

            initDeclaration(this@withBodyFrom, propertyWithBody)
            resolvePhase = propertyResolvePhase
        }
    }

    private inline fun getAccessorToUse(
        originalProperty: FirProperty,
        replacementProperty: FirProperty,
        getAccessor: (FirProperty) -> FirPropertyAccessor?
    ): FirPropertyAccessor? {
        val originalAccessor = getAccessor(originalProperty)
        val replacementAccessor = getAccessor(replacementProperty)

        // accessor has a header type, and we want it type to be resolved
        return if (originalAccessor != null && replacementAccessor != null) {
            buildPropertyAccessorCopy(originalAccessor) {
                body = replacementAccessor.body
                symbol = replacementAccessor.symbol
                initDeclaration(originalAccessor, replacementAccessor)
            }.apply { reassignAllReturnTargets(replacementAccessor) }
        } else {
            replacementAccessor
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

    private fun FirScriptBuilder.initDeclaration(
        originalDeclaration: FirDeclaration,
        builtDeclaration: FirDeclaration,
    ) {
        resolvePhase = minOf(originalDeclaration.resolvePhase, FirResolvePhase.DECLARATIONS)
        source = builtDeclaration.source
        moduleData = originalDeclaration.moduleData
    }

    private fun FirFunction.reassignAllReturnTargets(from: FirFunction) {
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
