/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayLiteral
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.expectedConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.RXJAVA3_ANNOTATIONS
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind

internal val JavaModifierListOwner.modality: Modality
    get() = when {
        this is JavaField -> Modality.FINAL
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

internal fun Any?.createConstantOrError(session: FirSession, expectedConeType: ConeKotlinType? = null): FirExpression {
    val value = if (this is Int && expectedConeType != null) {
        // special case for Java literals in annotation default values:
        // literal value is always integer, but an expected parameter type can be any other number type
        when {
            expectedConeType.isByte -> this.toByte()
            expectedConeType.isShort -> this.toShort()
            expectedConeType.isLong -> this.toLong()
            else -> this
        }
    } else this
    return value.createConstantIfAny(session) ?: buildErrorExpression {
        diagnostic = ConeSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java)
    }
}

internal fun Any?.createConstantIfAny(session: FirSession, unsigned: Boolean = false): FirExpression? {
    return when (this) {
        is Byte -> buildLiteralExpression(
            null, if (unsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte, this, setType = true
        )
        is Short -> buildLiteralExpression(
            null, if (unsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short, this, setType = true
        )
        is Int -> buildLiteralExpression(
            null, if (unsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int, this, setType = true
        )
        is Long -> buildLiteralExpression(
            null, if (unsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long, this, setType = true
        )
        is Char -> buildLiteralExpression(
            null, ConstantValueKind.Char, this, setType = true
        )
        is Float -> buildLiteralExpression(
            null, ConstantValueKind.Float, this, setType = true
        )
        is Double -> buildLiteralExpression(
            null, ConstantValueKind.Double, this, setType = true
        )
        is Boolean -> buildLiteralExpression(
            null, ConstantValueKind.Boolean, this, setType = true
        )
        is String -> buildLiteralExpression(
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
        null -> buildLiteralExpression(
            null, ConstantValueKind.Null, null, setType = true
        )

        else -> null
    }
}

private fun <T> List<T>.createArrayLiteral(session: FirSession, kind: ConstantValueKind): FirArrayLiteral {
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

fun FirProperty.hasJvmFieldAnnotation(session: FirSession): Boolean =
    backingField?.annotations?.any { it.isJvmFieldAnnotation(session) } == true

fun FirAnnotation.isJvmFieldAnnotation(session: FirSession): Boolean =
    toAnnotationClassId(session) == JvmStandardClassIds.Annotations.JvmField

// The implementation is different from `FirAnnotationContainer.getAnnotationsByClassId` because it doesn't expand typealiases
// The reason is that some usesites do not have access to the session. For the intended use for main function detection it seems fine
// for now, but we may need to reimplement it in the future. See KT-67634
// See also the comment in the body, relevant for the use in the const evaluator.
private fun FirDeclaration.findAnnotationByClassId(classId: ClassId): FirAnnotation? {
    return annotations.firstOrNull {
        // Access to type must be through `coneTypeOrNull`.
        // Even if `JvmName` is in the list of annotations that must be resoled for compilation, we still could try to access some user
        // annotations that could be not resolved.
        it.annotationTypeRef.coneTypeOrNull?.classId == classId
    }
}

fun FirDeclaration.findJvmNameAnnotation(): FirAnnotation? = findAnnotationByClassId(JvmStandardClassIds.Annotations.JvmName)
fun FirDeclaration.findJvmStaticAnnotation(): FirAnnotation? = findAnnotationByClassId(JvmStandardClassIds.Annotations.JvmStatic)

fun FirDeclaration.findJvmNameValue(): String? =
    findJvmNameAnnotation()?.findArgumentByName(StandardNames.NAME)?.let {
        (it as? FirLiteralExpression)?.value as? String
    }
