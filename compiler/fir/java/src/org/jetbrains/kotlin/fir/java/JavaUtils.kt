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
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayOfCall
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.expectedConeType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.createArrayType
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

internal fun Any?.createConstantOrError(session: FirSession): FirExpression {
    return createConstantIfAny(session) ?: buildErrorExpression {
        diagnostic = ConeSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java)
    }
}

internal fun Any?.createConstantIfAny(session: FirSession, unsigned: Boolean = false): FirExpression? {
    return when (this) {
        is Byte -> buildConstExpression(null, if (unsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte, this).setProperType(session)
        is Short -> buildConstExpression(null, if (unsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short, this).setProperType(session)
        is Int -> buildConstExpression(null, if (unsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int, this).setProperType(session)
        is Long -> buildConstExpression(null, if (unsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long, this).setProperType(session)
        is Char -> buildConstExpression(null, ConstantValueKind.Char, this).setProperType(session)
        is Float -> buildConstExpression(null, ConstantValueKind.Float, this).setProperType(session)
        is Double -> buildConstExpression(null, ConstantValueKind.Double, this).setProperType(session)
        is Boolean -> buildConstExpression(null, ConstantValueKind.Boolean, this).setProperType(session)
        is String -> buildConstExpression(null, ConstantValueKind.String, this).setProperType(session)
        is ByteArray -> toList().createArrayOfCall(session, ConstantValueKind.Byte)
        is ShortArray -> toList().createArrayOfCall(session, ConstantValueKind.Short)
        is IntArray -> toList().createArrayOfCall(session, ConstantValueKind.Int)
        is LongArray -> toList().createArrayOfCall(session, ConstantValueKind.Long)
        is CharArray -> toList().createArrayOfCall(session, ConstantValueKind.Char)
        is FloatArray -> toList().createArrayOfCall(session, ConstantValueKind.Float)
        is DoubleArray -> toList().createArrayOfCall(session, ConstantValueKind.Double)
        is BooleanArray -> toList().createArrayOfCall(session, ConstantValueKind.Boolean)
        null -> buildConstExpression(null, ConstantValueKind.Null, null).setProperType(session)

        else -> null
    }
}

private fun <T> List<T>.createArrayOfCall(session: FirSession, kind: ConstantValueKind<T>): FirArrayOfCall {
    return buildArrayOfCall {
        argumentList = buildArgumentList {
            for (element in this@createArrayOfCall) {
                arguments += element.createConstantOrError(session)
            }
        }
        typeRef = buildResolvedTypeRef {
            type = kind.expectedConeType(session).createArrayType()
        }
    }
}

private fun FirConstExpression<*>.setProperType(session: FirSession): FirConstExpression<*> {
    val typeRef = buildResolvedTypeRef {
        type = kind.expectedConeType(session)
    }
    replaceTypeRef(typeRef)
    return this
}

// For now, it's supported only for RxJava3 annotations, see KT-53041
fun extractNullabilityAnnotationOnBoundedWildcard(wildcardType: JavaWildcardType): JavaAnnotation? {
    require(wildcardType.bound != null) { "Nullability annotations on unbounded wildcards aren't supported" }
    return wildcardType.annotations.find { annotation -> RXJAVA3_ANNOTATIONS.any { annotation.classId?.asSingleFqName() == it } }
}
