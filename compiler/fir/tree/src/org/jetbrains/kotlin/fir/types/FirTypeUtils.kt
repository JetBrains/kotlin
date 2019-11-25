/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId

inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeUnsafe() = (this as FirResolvedTypeRef).type as T
inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeSafe() = (this as? FirResolvedTypeRef)?.type as? T

val FirTypeRef.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)
val FirTypeRef.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)
val FirTypeRef.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)
val FirTypeRef.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)
val FirTypeRef.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)
val FirTypeRef.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, false)

private fun FirTypeRef.isBuiltinType(classId: ClassId, isNullable: Boolean): Boolean {
    val type = when (this) {
        is FirImplicitBuiltinTypeRef -> type
        is FirResolvedTypeRef -> type as? ConeClassLikeType ?: return false
        else -> return false
    }
    return type.lookupTag.classId == classId && type.isNullable == isNullable
}

val FirFunctionTypeRef.parametersCount: Int
    get() = if (receiverTypeRef != null)
        valueParameters.size + 1
    else
        valueParameters.size

const val EXTENSION_FUNCTION_ANNOTATION = "kotlin/ExtensionFunctionType"

fun FirTypeRef.isExtensionFunctionType(): Boolean {
    return annotations.any {
        it.isExtensionFunctionAnnotationCall
    }
}

val FirAnnotationCall.isExtensionFunctionAnnotationCall: Boolean
    get() = (this as? FirAnnotationCall)?.let {
        (it.annotationTypeRef as? FirResolvedTypeRef)?.let {
            (it.type as? ConeClassLikeType)?.let {
                it.lookupTag.classId.asString() == EXTENSION_FUNCTION_ANNOTATION
            }
        }
    } == true


fun List<FirAnnotationCall>.dropExtensionFunctionAnnotation(): List<FirAnnotationCall> {
    return filterNot { it.isExtensionFunctionAnnotationCall }
}