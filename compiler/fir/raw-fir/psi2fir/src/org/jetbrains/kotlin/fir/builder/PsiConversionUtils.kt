/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.psi.*

internal fun KtWhenCondition.toFirWhenCondition(
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    val baseSource = this.toFirPsiSourceElement()
    val firSubjectExpression = buildWhenSubjectExpression {
        source = baseSource
        whenSubject = subject
    }
    return when (this) {
        is KtWhenConditionWithExpression -> {
            buildOperatorCall {
                source = expression?.toFirPsiSourceElement()
                operation = FirOperation.EQ
                argumentList = buildBinaryArgumentList(
                    firSubjectExpression, expression.convert("No expression in condition with expression")
                )
            }
        }
        is KtWhenConditionInRange -> {
            val firRange = rangeExpression.convert("No range in condition with range")
            firRange.generateContainsOperation(
                firSubjectExpression,
                isNegated,
                rangeExpression?.toFirPsiSourceElement(),
                operationReference.toFirPsiSourceElement()
            )
        }
        is KtWhenConditionIsPattern -> {
            buildTypeOperatorCall {
                source = typeReference?.toFirPsiSourceElement()
                operation = if (isNegated) FirOperation.NOT_IS else FirOperation.IS
                conversionTypeRef = typeReference.toFirOrErrorTypeRef()
                argumentList = buildUnaryArgumentList(firSubjectExpression)
            }
        }
        else -> {
            buildErrorExpression(baseSource, ConeSimpleDiagnostic("Unsupported when condition: ${this.javaClass}", DiagnosticKind.Syntax))
        }
    }
}

internal fun Array<KtWhenCondition>.toFirWhenCondition(
    baseSource: FirSourceElement?,
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    var firCondition: FirExpression? = null
    for (condition in this) {
        val firConditionElement = condition.toFirWhenCondition(subject, convert, toFirOrErrorTypeRef)
        firCondition = when (firCondition) {
            null -> firConditionElement
            else -> firCondition.generateLazyLogicalOperation(
                firConditionElement, false, baseSource,
            )
        }
    }
    return firCondition!!
}

internal fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable<*>,
    tmpVariable: Boolean,
    extractAnnotationsTo: KtAnnotated.(FirAnnotationContainerBuilder) -> Unit,
    toFirOrImplicitTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    return buildBlock {
        source = multiDeclaration.toFirPsiSourceElement()
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            val entrySource = entry.toFirPsiSourceElement()
            val name = entry.nameAsSafeName
            statements += buildProperty {
                source = entrySource
                this.session = session
                returnTypeRef = entry.typeReference.toFirOrImplicitTypeRef()
                this.name = name
                initializer = buildComponentCall {
                    source = entrySource
                    explicitReceiver = generateResolvedAccessExpression(entrySource, container)
                    componentIndex = index + 1
                }
                this.isVar = isVar
                isLocal = true
                status = FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
                symbol = FirPropertySymbol(name)
                entry.extractAnnotationsTo(this)
            }
        }
    }
}