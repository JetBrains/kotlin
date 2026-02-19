/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.buildWhenSubjectAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.builder.buildTypeOperatorCall
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.toKtPsiSourceElement

internal fun KtWhenCondition.toFirWhenCondition(
    subjectVariable: FirVariable?,
    convert: KtExpression?.(String, fallbackSource: KtElement) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    val firSubjectSource = this.toKtPsiSourceElement(KtFakeSourceElementKind.WhenGeneratedSubject)
    val firSubjectExpression = buildWhenSubjectAccess(firSubjectSource, subjectVariable)

    return when (this) {
        is KtWhenConditionWithExpression -> {
            buildEqualityOperatorCall {
                source = (expression ?: firstChild)?.toKtPsiSourceElement(KtFakeSourceElementKind.WhenCondition)
                operation = FirOperation.EQ
                argumentList = buildBinaryArgumentList(
                    firSubjectExpression, expression.convert("No expression in condition with expression", this@toFirWhenCondition)
                )
            }
        }
        is KtWhenConditionInRange -> {
            val firRange = rangeExpression.convert("No range in condition with range", this)
            firRange.generateContainsOperation(
                firSubjectExpression,
                isNegated,
                this@toFirWhenCondition.toKtPsiSourceElement(),
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
    subjectVariable: FirVariable?,
    convert: KtExpression?.(String, fallbackSource: KtElement) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    val conditions = this.map { condition ->
        condition.toFirWhenCondition(subjectVariable, convert, toFirOrErrorTypeRef)
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
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
    extractAnnotationsTo: (KtAnnotated.(FirAnnotationContainerBuilder) -> Unit),
): FirVariable =
    buildProperty {
        this.source = source
        this.moduleData = moduleData
        this.origin = origin
        returnTypeRef = typeRef ?: FirImplicitTypeRefImplWithoutSource
        this.name = name
        this.initializer = initializer
        symbol = FirLocalPropertySymbol()
        isVar = false
        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
        isLocal = true
        (source.psi as? KtAnnotated)?.extractAnnotationsTo(this)
    }

internal fun generateTemporaryVariable(
    moduleData: FirModuleData,
    source: KtSourceElement?,
    specialName: String,
    initializer: FirExpression,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
    extractAnnotationsTo: (KtAnnotated.(FirAnnotationContainerBuilder) -> Unit),
): FirVariable =
    generateTemporaryVariable(
        moduleData,
        source,
        Name.special("<$specialName>"),
        initializer,
        typeRef = null,
        origin,
        extractAnnotationsTo,
    )

context(c: DestructuringContext<KtDestructuringDeclarationEntry>)
internal fun AbstractRawFirBuilder<*>.generateDestructuringBlock(
    moduleData: FirModuleData,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean,
): FirBlock {
    return buildBlock {
        source = multiDeclaration.toKtPsiSourceElement(KtFakeSourceElementKind.DestructuringBlock)
        addDestructuringVariables(
            statements,
            moduleData,
            multiDeclaration,
            container,
            tmpVariable,
            forceLocal = false,
        )
    }
}

context(c: DestructuringContext<KtDestructuringDeclarationEntry>)
internal fun AbstractRawFirBuilder<*>.addDestructuringVariables(
    destination: MutableList<in FirVariable>,
    moduleData: FirModuleData,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean,
    forceLocal: Boolean,
    configure: (FirVariable) -> Unit = {}
) {
    addDestructuringVariables(
        destination,
        moduleData,
        container,
        multiDeclaration.entries,
        isNameBased = !multiDeclaration.hasSquareBrackets() && (multiDeclaration.isFullForm || nameBasedDestructuringShortForm),
        isTmpVariable = tmpVariable,
        forceLocal = forceLocal,
        configure,
    )
}
