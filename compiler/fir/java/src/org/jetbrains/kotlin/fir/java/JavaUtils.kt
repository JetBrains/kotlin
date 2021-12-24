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
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaModifierListOwner
import org.jetbrains.kotlin.types.ConstantValueKind

internal val JavaModifierListOwner.modality: Modality
    get() = when {
        isAbstract -> Modality.ABSTRACT
        isFinal -> Modality.FINAL
        else -> Modality.OPEN
    }

internal val JavaClass.modality: Modality
    get() = when {
        isSealed -> Modality.SEALED
        isAbstract -> Modality.ABSTRACT
        isFinal -> Modality.FINAL
        else -> Modality.OPEN
    }

internal val JavaClass.classKind: ClassKind
    get() = when {
        isAnnotationType -> ClassKind.ANNOTATION_CLASS
        isInterface -> ClassKind.INTERFACE
        isEnum -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

internal fun Any?.createConstantOrError(session: FirSession): FirExpression {
    return createConstantIfAny(session) ?: buildErrorExpression {
        diagnostic = ConeSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java)
    }
}

internal fun Any?.createConstantIfAny(session: FirSession): FirExpression? {
    return when (this) {
        is Byte -> buildConstExpression(null, ConstantValueKind.Byte, this).setProperType(session)
        is Short -> buildConstExpression(null, ConstantValueKind.Short, this).setProperType(session)
        is Int -> buildConstExpression(null, ConstantValueKind.Int, this).setProperType(session)
        is Long -> buildConstExpression(null, ConstantValueKind.Long, this).setProperType(session)
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
