/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal fun KtWhenCondition.toFirWhenCondition(
    whenRefWithSubject: FirExpressionRef<FirWhenExpression>,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    val firSubjectSource = this.toKtPsiSourceElement(KtFakeSourceElementKind.WhenGeneratedSubject)
    val firSubjectExpression = buildWhenSubjectExpression {
        source = firSubjectSource
        whenRef = whenRefWithSubject
    }
    return when (this) {
        is KtWhenConditionWithExpression -> {
            buildEqualityOperatorCall {
                source = expression?.toKtPsiSourceElement(KtFakeSourceElementKind.WhenCondition)
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
                this@toFirWhenCondition.toKtPsiSourceElement(KtFakeSourceElementKind.WhenCondition),
                operationReference.toKtPsiSourceElement()
            )
        }
        is KtWhenConditionIsPattern -> {
            buildTypeOperatorCall {
                source = this@toFirWhenCondition.toKtPsiSourceElement()
                operation = if (isNegated) FirOperation.NOT_IS else FirOperation.IS
                conversionTypeRef = typeReference.toFirOrErrorTypeRef()
                argumentList = buildUnaryArgumentList(firSubjectExpression)
            }
        }
        else -> {
            buildErrorExpression(firSubjectSource, ConeSyntaxDiagnostic("Unsupported when condition: ${this.javaClass}"))
        }
    }
}

internal fun Array<KtWhenCondition>.toFirWhenCondition(
    subject: FirExpressionRef<FirWhenExpression>,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    val conditions = this.map { condition ->
        condition.toFirWhenCondition(subject, convert, toFirOrErrorTypeRef)
    }

    require(conditions.isNotEmpty())
    // We build balanced tree of OR expressions to ensure we won't run out of stack
    // while processing huge conditions
    return buildBalancedOrExpressionTree(conditions)
}

internal fun generateTemporaryVariable(
    moduleData: FirModuleData,
    source: KtSourceElement?,
    name: Name,
    initializer: FirExpression,
    typeRef: FirTypeRef? = null,
    extractAnnotationsTo: (KtAnnotated.(FirAnnotationContainerBuilder) -> Unit),
): FirVariable =
    buildProperty {
        this.source = source
        this.moduleData = moduleData
        origin = FirDeclarationOrigin.Source
        returnTypeRef = typeRef ?: FirImplicitTypeRefImplWithoutSource
        this.name = name
        this.initializer = initializer
        symbol = FirPropertySymbol(name)
        isVar = false
        isLocal = true
        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
        (source.psi as? KtAnnotated)?.extractAnnotationsTo(this)
    }

internal fun generateTemporaryVariable(
    moduleData: FirModuleData,
    source: KtSourceElement?,
    specialName: String,
    initializer: FirExpression,
    extractAnnotationsTo: (KtAnnotated.(FirAnnotationContainerBuilder) -> Unit),
): FirVariable =
    generateTemporaryVariable(
        moduleData,
        source,
        Name.special("<$specialName>"),
        initializer,
        null,
        extractAnnotationsTo,
    )

internal fun generateDestructuringBlock(
    moduleData: FirModuleData,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean,
    extractAnnotationsTo: KtAnnotated.(FirAnnotationContainerBuilder) -> Unit,
    toFirOrImplicitTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirBlock {
    return buildBlock {
        source = multiDeclaration.toKtPsiSourceElement()
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            if (entry.nameIdentifier?.text == "_") continue
            val entrySource = entry.toKtPsiSourceElement()
            val name = entry.nameAsSafeName
            statements += buildProperty {
                source = entrySource
                this.moduleData = moduleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = entry.typeReference.toFirOrImplicitTypeRef()
                this.name = name
                initializer = buildComponentCall {
                    val componentCallSource = entrySource.fakeElement(KtFakeSourceElementKind.DesugaredComponentFunctionCall)
                    source = componentCallSource
                    explicitReceiver = generateResolvedAccessExpression(componentCallSource, container)
                    componentIndex = index + 1
                }
                this.isVar = isVar
                isLocal = true
                status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                symbol = FirPropertySymbol(name)
                entry.extractAnnotationsTo(this)
            }
        }
    }
}
