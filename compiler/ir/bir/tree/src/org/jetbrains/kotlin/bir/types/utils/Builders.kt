/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.symbols.BirTypeAliasSymbol
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.compactIfPossible
import org.jetbrains.kotlin.types.model.CaptureStatus

class BirSimpleTypeBuilder {
    var kotlinType: KotlinType? = null
    var classifier: BirClassifierSymbol? = null
    var nullability = SimpleTypeNullability.NOT_SPECIFIED
    var arguments: List<BirTypeArgument> = emptyList()
    var annotations: List<BirConstructorCall> = emptyList()
    var abbreviation: BirTypeAbbreviation? = null

    var captureStatus: CaptureStatus? = null
    var capturedLowerType: BirType? = null
    var capturedTypeConstructor: BirCapturedType.Constructor? = null
}

fun BirSimpleType.toBuilder() =
    BirSimpleTypeBuilder().also { b ->
        b.kotlinType = originalKotlinType
        if (this is BirCapturedType) {
            b.captureStatus = captureStatus
            b.capturedLowerType = lowerType
            b.capturedTypeConstructor = constructor
        } else {
            b.classifier = classifier
        }
        b.classifier = classifier
        b.nullability = nullability
        b.arguments = arguments
        b.annotations = annotations
        b.abbreviation = abbreviation
    }

fun BirSimpleTypeBuilder.buildSimpleType(): BirSimpleType =
    if (classifier == null) {
        check(captureStatus != null && capturedTypeConstructor != null) {
            "Neither classifier nor captured type constructor is provided"
        }
        check(arguments.isEmpty()) {
            "Arguments should be empty when creating a captured type: ${capturedTypeConstructor?.argument?.render()}"
        }
        BirCapturedType(
            captureStatus!!,
            capturedLowerType,
            capturedTypeConstructor!!.argument,
            capturedTypeConstructor!!.typeParameter,
            nullability,
            annotations.compactIfPossible(),
            abbreviation,
        ).apply {
            constructor.initSuperTypes(capturedTypeConstructor!!.superTypes)
        }
    } else {
        check(captureStatus == null && capturedTypeConstructor == null) {
            "Both classifier and captured type constructor are provided"
        }
        BirSimpleTypeImpl(
            kotlinType,
            classifier ?: throw AssertionError("Classifier not provided"),
            nullability,
            arguments.compactIfPossible(),
            annotations.compactIfPossible(),
            abbreviation
        )
    }

fun BirSimpleTypeBuilder.buildTypeProjection(variance: Variance) =
    if (variance == Variance.INVARIANT)
        buildSimpleType()
    else
        BirTypeProjectionImpl(buildSimpleType(), variance)

inline fun BirSimpleType.buildSimpleType(b: BirSimpleTypeBuilder.() -> Unit): BirSimpleType =
    toBuilder().apply(b).buildSimpleType()


class BirTypeAbbreviationBuilder {
    var typeAlias: BirTypeAliasSymbol? = null
    var hasQuestionMark: Boolean = false
    var arguments: List<BirTypeArgument> = emptyList()
    var annotations: List<BirConstructorCall> = emptyList()
}

fun BirTypeAbbreviation.toBuilder() =
    BirTypeAbbreviationBuilder().also { b ->
        b.typeAlias = typeAlias
        b.hasQuestionMark = hasQuestionMark
        b.arguments = arguments
        b.annotations = annotations
    }

fun BirTypeAbbreviationBuilder.build() =
    BirTypeAbbreviation(
        typeAlias ?: throw AssertionError("typeAlias not provided"),
        hasQuestionMark, arguments, annotations
    )