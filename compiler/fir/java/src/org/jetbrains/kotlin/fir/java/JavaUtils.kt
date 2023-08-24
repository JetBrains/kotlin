/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayLiteral
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.expectedConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.RXJAVA3_ANNOTATIONS
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaModifierListOwner
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType
import org.jetbrains.kotlin.types.ConstantValueKind

internal val JavaModifierListOwner.modality: Modality
    get() = when {
        isAbstract -> Modality.ABSTRACT
        isFinal -> Modality.FINAL
        else -> Modality.OPEN
    }

val JavaClass.modality: Modality
    get() = when {
        isAnnotationType || isEnum -> Modality.FINAL
        isSealed -> Modality.SEALED
        isAbstract -> Modality.ABSTRACT
        isFinal -> Modality.FINAL
        else -> Modality.OPEN
    }

val JavaClass.classKind: ClassKind
    get() = when {
        isAnnotationType -> ClassKind.ANNOTATION_CLASS
        isInterface -> ClassKind.INTERFACE
        isEnum -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

fun JavaClass.hasMetadataAnnotation(): Boolean =
    annotations.any { it.isResolvedTo(JvmAnnotationNames.METADATA_FQ_NAME) }

internal fun Any?.createConstantOrError(session: FirSession, expectedTypeRef: FirTypeRef? = null): FirExpression {
    val coneType = expectedTypeRef?.coneTypeOrNull
    val value = if (this is Int && coneType != null) {
        // special case for Java literals in annotation default values:
        // literal value is always integer, but an expected parameter type can be any other number type
        when {
            coneType.isByte -> this.toByte()
            coneType.isShort -> this.toShort()
            coneType.isLong -> this.toLong()
            else -> this
        }
    } else this
    return value.createConstantIfAny(session) ?: buildErrorExpression {
        diagnostic = ConeSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java)
    }
}

internal fun Any?.createConstantIfAny(session: FirSession, unsigned: Boolean = false): FirExpression? {
    return when (this) {
        is Byte -> buildConstExpression(
            null, if (unsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte, this, setType = true
        )
        is Short -> buildConstExpression(
            null, if (unsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short, this, setType = true
        )
        is Int -> buildConstExpression(
            null, if (unsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int, this, setType = true
        )
        is Long -> buildConstExpression(
            null, if (unsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long, this, setType = true
        )
        is Char -> buildConstExpression(
            null, ConstantValueKind.Char, this, setType = true
        )
        is Float -> buildConstExpression(
            null, ConstantValueKind.Float, this, setType = true
        )
        is Double -> buildConstExpression(
            null, ConstantValueKind.Double, this, setType = true
        )
        is Boolean -> buildConstExpression(
            null, ConstantValueKind.Boolean, this, setType = true
        )
        is String -> buildConstExpression(
            null, ConstantValueKind.String, this, setType = true
        )
        is ByteArray -> toList().createArrayLiteral(session, ConstantValueKind.Byte)
        is ShortArray -> toList().createArrayLiteral(session, ConstantValueKind.Short)
        is IntArray -> toList().createArrayLiteral(session, ConstantValueKind.Int)
        is LongArray -> toList().createArrayLiteral(session, ConstantValueKind.Long)
        is CharArray -> toList().createArrayLiteral(session, ConstantValueKind.Char)
        is FloatArray -> toList().createArrayLiteral(session, ConstantValueKind.Float)
        is DoubleArray -> toList().createArrayLiteral(session, ConstantValueKind.Double)
        is BooleanArray -> toList().createArrayLiteral(session, ConstantValueKind.Boolean)
        null -> buildConstExpression(
            null, ConstantValueKind.Null, null, setType = true
        )

        else -> null
    }
}

private fun <T> List<T>.createArrayLiteral(session: FirSession, kind: ConstantValueKind<T>): FirArrayLiteral {
    return buildArrayLiteral {
        argumentList = buildArgumentList {
            for (element in this@createArrayLiteral) {
                arguments += element.createConstantOrError(session)
            }
        }
        coneTypeOrNull = kind.expectedConeType(session).createArrayType()
    }
}

// For now, it's supported only for RxJava3 annotations, see KT-53041
fun extractNullabilityAnnotationOnBoundedWildcard(wildcardType: JavaWildcardType): JavaAnnotation? {
    require(wildcardType.bound != null) { "Nullability annotations on unbounded wildcards aren't supported" }
    return wildcardType.annotations.find { annotation -> RXJAVA3_ANNOTATIONS.any { annotation.classId?.asSingleFqName() == it } }
}
