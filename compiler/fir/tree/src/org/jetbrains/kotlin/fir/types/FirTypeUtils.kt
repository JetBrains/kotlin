/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.fakeElement
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeUnsafe() = (this as FirResolvedTypeRef).type as T
@OptIn(ExperimentalContracts::class)
inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeSafe(): T? {
    contract {
        returnsNotNull() implies (this@coneTypeSafe is FirResolvedTypeRef)
    }
    return (this as? FirResolvedTypeRef)?.type as? T
}
inline val FirTypeRef.coneType: ConeKotlinType get() = coneTypeUnsafe()

val FirTypeRef.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)
val FirTypeRef.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)
val FirTypeRef.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)
val FirTypeRef.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)
val FirTypeRef.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)
val FirTypeRef.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, false)
val FirTypeRef.isEnum: Boolean get() = isBuiltinType(StandardClassIds.Enum, false)
val FirTypeRef.isArrayType: Boolean
    get() =
        isBuiltinType(StandardClassIds.Array, false) ||
                StandardClassIds.primitiveArrayTypeByElementType.values.any { isBuiltinType(it, false) }

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

fun ConeClassLikeType.toConstKind(): FirConstKind<*>? = when (lookupTag.classId) {
    StandardClassIds.Byte -> FirConstKind.Byte
    StandardClassIds.Short -> FirConstKind.Short
    StandardClassIds.Int -> FirConstKind.Int
    StandardClassIds.Long -> FirConstKind.Long

    StandardClassIds.UInt -> FirConstKind.UnsignedInt
    StandardClassIds.ULong -> FirConstKind.UnsignedLong
    StandardClassIds.UShort -> FirConstKind.UnsignedShort
    StandardClassIds.UByte -> FirConstKind.UnsignedByte
    else -> null
}

fun List<FirAnnotationCall>.computeTypeAttributes(): ConeAttributes {
    if (this.isEmpty()) return ConeAttributes.Empty
    val attributes = mutableListOf<ConeAttribute<*>>()
    for (annotation in this) {
        val type = annotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
        when (type.lookupTag.classId) {
            CompilerConeAttributes.Exact.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.Exact
            CompilerConeAttributes.NoInfer.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.NoInfer
            CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.ExtensionFunctionType
        }
    }
    return ConeAttributes.create(attributes)
}

fun FirTypeRef.withReplacedConeType(
    newType: ConeKotlinType?,
    firFakeSourceElementKind: FirFakeSourceElementKind? = null
): FirResolvedTypeRef {
    require(this is FirResolvedTypeRef)
    if (newType == null) return this

    return buildResolvedTypeRef {
        source = if (firFakeSourceElementKind != null)
            this@withReplacedConeType.source?.fakeElement(firFakeSourceElementKind)
        else
            this@withReplacedConeType.source
        type = newType
        annotations += this@withReplacedConeType.annotations
    }
}

fun FirTypeRef.approximated(
    typeApproximator: AbstractTypeApproximator,
    toSuper: Boolean,
    conf: TypeApproximatorConfiguration = TypeApproximatorConfiguration.PublicDeclaration
): FirTypeRef {
    val approximatedType = if (toSuper)
        typeApproximator.approximateToSuperType(coneType, conf)
    else
        typeApproximator.approximateToSubType(coneType, conf)
    return withReplacedConeType(approximatedType as? ConeKotlinType)
}
