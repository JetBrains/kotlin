/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeUnsafe(): T = (this as FirResolvedTypeRef).type as T

@OptIn(ExperimentalContracts::class)
inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeSafe(): T? {
    contract {
        returnsNotNull() implies (this@coneTypeSafe is FirResolvedTypeRef)
    }
    return (this as? FirResolvedTypeRef)?.type as? T
}

val FirTypeRef.coneType: ConeKotlinType
    get() = coneTypeSafe()
        ?: errorWithAttachment("Expected ${FirResolvedTypeRef::class.simpleName} with ${ConeKotlinType::class.simpleName} but was ${this::class.simpleName}") {
            withFirEntry("typeRef", this@coneType)
        }

val FirTypeRef.coneTypeOrNull: ConeKotlinType?
    get() = coneTypeSafe()

@OptIn(UnresolvedExpressionTypeAccess::class)
val FirExpression.resolvedType: ConeKotlinType
    get() = coneTypeOrNull
        ?: errorWithAttachment("Expected expression '${this::class.simpleName}' to be resolved") {
            withFirEntry("expression", this@resolvedType)
        }

@OptIn(UnresolvedExpressionTypeAccess::class)
val FirExpression.isResolved: Boolean get() = coneTypeOrNull != null

@RequiresOptIn(
    "This type check never expands type aliases. Use with care (probably Ok for expression & constructor types). " +
            "Generally this.coneType.fullyExpandedType(session).isSomeType is better"
)
annotation class UnexpandedTypeCheck

@UnexpandedTypeCheck
val FirTypeRef.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)

@UnexpandedTypeCheck
val FirTypeRef.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)

@UnexpandedTypeCheck
val FirTypeRef.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)

@UnexpandedTypeCheck
val FirTypeRef.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)

@UnexpandedTypeCheck
val FirTypeRef.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)

@UnexpandedTypeCheck
val FirTypeRef.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, false)

@UnexpandedTypeCheck
val FirTypeRef.isInt: Boolean get() = isBuiltinType(StandardClassIds.Int, false)

@UnexpandedTypeCheck
val FirTypeRef.isString: Boolean get() = isBuiltinType(StandardClassIds.String, false)

@UnexpandedTypeCheck
val FirTypeRef.isEnum: Boolean get() = isBuiltinType(StandardClassIds.Enum, false)

@UnexpandedTypeCheck
val FirTypeRef.isArrayType: Boolean
    get() =
        isBuiltinType(StandardClassIds.Array, false)
                || StandardClassIds.primitiveArrayTypeByElementType.values.any { isBuiltinType(it, false) }
                || StandardClassIds.unsignedArrayTypeByElementType.values.any { isBuiltinType(it, false) }

val FirExpression.isNullLiteral: Boolean
    get() = this is FirLiteralExpression<*> &&
            this.kind == ConstantValueKind.Null &&
            this.value == null &&
            this.source != null

@OptIn(ExperimentalContracts::class)
fun FirExpression.isStableSmartcast(): Boolean {
    contract {
        returns(true) implies (this@isStableSmartcast is FirSmartCastExpression)
    }
    return this is FirSmartCastExpression && this.isStable
}

private val FirTypeRef.lookupTagBasedOrNull: ConeLookupTagBasedType?
    get() = when (this) {
        is FirImplicitBuiltinTypeRef -> type
        is FirResolvedTypeRef -> type as? ConeLookupTagBasedType
        else -> null
    }

private fun FirTypeRef.isBuiltinType(classId: ClassId, isNullable: Boolean): Boolean {
    val type = this.lookupTagBasedOrNull ?: return false
    return (type as? ConeClassLikeType)?.lookupTag?.classId == classId && type.isNullable == isNullable
}

val FirTypeRef.isMarkedNullable: Boolean?
    get() = if (this is FirTypeRefWithNullability) this.isMarkedNullable else lookupTagBasedOrNull?.isMarkedNullable

val FirFunctionTypeRef.parametersCount: Int
    get() = if (receiverTypeRef != null)
        parameters.size + contextReceiverTypeRefs.size + 1
    else
        parameters.size + contextReceiverTypeRefs.size

private fun FirAnnotation.isOfType(classId: ClassId): Boolean {
    return (annotationTypeRef as? FirResolvedTypeRef)?.let { typeRef ->
        (typeRef.type as? ConeClassLikeType)?.let {
            it.lookupTag.classId == classId
        }
    } == true
}

val FirAnnotation.isExtensionFunctionAnnotationCall: Boolean
    get() = isOfType(StandardClassIds.Annotations.ExtensionFunctionType)

fun List<FirAnnotation>.dropExtensionFunctionAnnotation(): List<FirAnnotation> {
    return filterNot { it.isExtensionFunctionAnnotationCall }
}

fun ConeClassLikeType.toConstKind(): ConstantValueKind<*>? = when (lookupTag.classId) {
    StandardClassIds.Byte -> ConstantValueKind.Byte
    StandardClassIds.Short -> ConstantValueKind.Short
    StandardClassIds.Int -> ConstantValueKind.Int
    StandardClassIds.Long -> ConstantValueKind.Long

    StandardClassIds.UInt -> ConstantValueKind.UnsignedInt
    StandardClassIds.ULong -> ConstantValueKind.UnsignedLong
    StandardClassIds.UShort -> ConstantValueKind.UnsignedShort
    StandardClassIds.UByte -> ConstantValueKind.UnsignedByte
    else -> null
}

fun FirTypeProjection.toConeTypeProjection(): ConeTypeProjection = when (this) {
    is FirStarProjection -> ConeStarProjection
    is FirTypeProjectionWithVariance -> {
        val type = typeRef.coneType
        type.toTypeProjection(this.variance)
    }
    else -> errorWithAttachment("Unexpected ${this::class.simpleName}") { withFirEntry("projection", this@toConeTypeProjection) }
}

fun ConeKotlinType.arrayElementType(checkUnsignedArrays: Boolean = true): ConeKotlinType? {
    return when (val argument = arrayElementTypeArgument(checkUnsignedArrays)) {
        is ConeKotlinTypeProjection -> argument.type
        else -> null
    }
}

fun ConeKotlinType.arrayElementTypeArgument(checkUnsignedArrays: Boolean = true): ConeTypeProjection? {
    val type = this.lowerBoundIfFlexible()
    if (type !is ConeClassLikeType) return null
    val classId = type.lookupTag.classId
    if (classId == StandardClassIds.Array) {
        return type.typeArguments.first()
    }
    val elementType = StandardClassIds.elementTypeByPrimitiveArrayType[classId] ?: runIf(checkUnsignedArrays) {
        StandardClassIds.elementTypeByUnsignedArrayType[classId]
    }
    if (elementType != null) {
        return elementType.constructClassLikeType(emptyArray(), isNullable = false)
    }

    return null
}