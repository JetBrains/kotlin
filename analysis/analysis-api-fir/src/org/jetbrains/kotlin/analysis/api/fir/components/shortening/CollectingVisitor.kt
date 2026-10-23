/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.shortening

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

internal class CollectingVisitor(private val collector: ElementsToShortenCollector) : FirVisitorVoid() {
    private val visitedProperty = mutableSetOf<FirProperty>()

    override fun visitValueParameter(valueParameter: FirValueParameter) {
        super.visitValueParameter(valueParameter)
        valueParameter.correspondingProperty?.let { visitProperty(it) }
    }

    override fun visitProperty(property: FirProperty) {
        if (visitedProperty.add(property)) {
            super.visitProperty(property)
        }
    }

    override fun visitScript(script: FirScript) {
        @OptIn(DirectDeclarationsAccess::class)
        script.declarations.forEach {
            it.accept(this)
        }
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        visitResolvedTypeRef(errorTypeRef)

        errorTypeRef.partiallyResolvedTypeRef?.accept(this)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        collector.processTypeRef(resolvedTypeRef)

        resolvedTypeRef.acceptChildren(this)
        resolvedTypeRef.delegatedTypeRef?.accept(this)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        super.visitResolvedQualifier(resolvedQualifier)

        collector.processTypeQualifier(resolvedQualifier)
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier) {
        super.visitErrorResolvedQualifier(errorResolvedQualifier)

        collector.processTypeQualifier(errorResolvedQualifier)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        super.visitFunctionCall(functionCall)

        collector.processFunctionCall(functionCall)
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
        super.visitPropertyAccessExpression(propertyAccessExpression)

        collector.processPropertyAccess(propertyAccessExpression)
    }

    override fun visitThisReference(thisReference: FirThisReference) {
        super.visitThisReference(thisReference)

        collector.processThisReference(thisReference)
    }
}
