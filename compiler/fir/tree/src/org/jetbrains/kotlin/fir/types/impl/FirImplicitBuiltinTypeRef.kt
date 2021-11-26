/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

sealed class FirImplicitBuiltinTypeRef(
    override val source: KtSourceElement?,
    val id: ClassId,
    typeArguments: Array<out ConeTypeProjection> = emptyArray(),
    isNullable: Boolean = false
) : FirResolvedTypeRef() {
    override val annotations: List<FirAnnotation>
        get() = emptyList()

    override val type: ConeClassLikeType = ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(id), typeArguments, isNullable)

    override val delegatedTypeRef: FirTypeRef?
        get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedTypeRef {
        return this
    }
}

class FirImplicitUnitTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Unit)

class FirImplicitAnyTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Any)

class FirImplicitNullableAnyTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Any, isNullable = true)

class FirImplicitEnumTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Enum)

class FirImplicitAnnotationTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Annotation)

class FirImplicitBooleanTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Boolean)

class FirImplicitByteTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Byte)

class FirImplicitShortTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Short)

class FirImplicitIntTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Int)

class FirImplicitLongTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Long)

class FirImplicitDoubleTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Double)

class FirImplicitFloatTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Float)

class FirImplicitNothingTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Nothing)

class FirImplicitNullableNothingTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Nothing, isNullable = true)

class FirImplicitCharTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Char)

class FirImplicitStringTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.String)

class FirImplicitThrowableTypeRef(
    source: KtSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Throwable)

class FirImplicitKPropertyTypeRef(
    source: KtSourceElement?,
    typeArgument: ConeTypeProjection
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.KProperty, arrayOf(typeArgument))

class FirImplicitKProperty0TypeRef(
    source: KtSourceElement?,
    propertyTypeArgument: ConeTypeProjection
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.KProperty0, arrayOf(propertyTypeArgument))

class FirImplicitKMutableProperty0TypeRef(
    source: KtSourceElement?,
    propertyTypeArgument: ConeTypeProjection
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.KMutableProperty0, arrayOf(propertyTypeArgument))

class FirImplicitKProperty1TypeRef(
    source: KtSourceElement?,
    receiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.KProperty1, arrayOf(receiverTypeArgument, propertyTypeArgument))

class FirImplicitKMutableProperty1TypeRef(
    source: KtSourceElement?,
    receiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.KMutableProperty1, arrayOf(receiverTypeArgument, propertyTypeArgument))

class FirImplicitKProperty2TypeRef(
    source: KtSourceElement?,
    dispatchReceiverTypeArgument: ConeTypeProjection,
    extensionReceiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : FirImplicitBuiltinTypeRef(
    source, StandardClassIds.KProperty2,
    arrayOf(dispatchReceiverTypeArgument, extensionReceiverTypeArgument, propertyTypeArgument)
)

class FirImplicitKMutableProperty2TypeRef(
    source: KtSourceElement?,
    dispatchReceiverTypeArgument: ConeTypeProjection,
    extensionReceiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : FirImplicitBuiltinTypeRef(
    source, StandardClassIds.KMutableProperty2,
    arrayOf(dispatchReceiverTypeArgument, extensionReceiverTypeArgument, propertyTypeArgument)
)

fun FirImplicitBuiltinTypeRef.withFakeSource(kind: KtFakeSourceElementKind): FirImplicitBuiltinTypeRef {
    val source = source ?: return this
    if (source.kind == kind) return this
    val newSource = source.fakeElement(kind)
    return when (this) {
        is FirImplicitUnitTypeRef -> FirImplicitUnitTypeRef(newSource)
        is FirImplicitAnyTypeRef -> FirImplicitAnyTypeRef(newSource)
        is FirImplicitNullableAnyTypeRef -> FirImplicitNullableAnyTypeRef(newSource)
        is FirImplicitEnumTypeRef -> FirImplicitEnumTypeRef(newSource)
        is FirImplicitAnnotationTypeRef -> FirImplicitAnnotationTypeRef(newSource)
        is FirImplicitBooleanTypeRef -> FirImplicitBooleanTypeRef(newSource)
        is FirImplicitByteTypeRef -> FirImplicitByteTypeRef(newSource)
        is FirImplicitShortTypeRef -> FirImplicitShortTypeRef(newSource)
        is FirImplicitIntTypeRef -> FirImplicitIntTypeRef(newSource)
        is FirImplicitLongTypeRef -> FirImplicitLongTypeRef(newSource)
        is FirImplicitDoubleTypeRef -> FirImplicitDoubleTypeRef(newSource)
        is FirImplicitFloatTypeRef -> FirImplicitFloatTypeRef(newSource)
        is FirImplicitNothingTypeRef -> FirImplicitNothingTypeRef(newSource)
        is FirImplicitNullableNothingTypeRef -> FirImplicitNullableNothingTypeRef(newSource)
        is FirImplicitCharTypeRef -> FirImplicitCharTypeRef(newSource)
        is FirImplicitStringTypeRef -> FirImplicitStringTypeRef(newSource)
        is FirImplicitThrowableTypeRef -> FirImplicitThrowableTypeRef(newSource)
        is FirImplicitKPropertyTypeRef -> FirImplicitKPropertyTypeRef(
            newSource,
            typeArgument = type.typeArguments[0]
        )
        is FirImplicitKProperty0TypeRef -> FirImplicitKProperty0TypeRef(
            newSource,
            propertyTypeArgument = type.typeArguments[0]
        )
        is FirImplicitKMutableProperty0TypeRef -> FirImplicitKMutableProperty0TypeRef(
            newSource,
            propertyTypeArgument = type.typeArguments[0]
        )
        is FirImplicitKProperty1TypeRef -> FirImplicitKProperty1TypeRef(
            newSource,
            receiverTypeArgument = type.typeArguments[0],
            propertyTypeArgument = type.typeArguments[1]
        )
        is FirImplicitKMutableProperty1TypeRef -> FirImplicitKMutableProperty1TypeRef(
            newSource,
            receiverTypeArgument = type.typeArguments[0],
            propertyTypeArgument = type.typeArguments[1]
        )
        is FirImplicitKProperty2TypeRef -> FirImplicitKProperty2TypeRef(
            newSource,
            dispatchReceiverTypeArgument = type.typeArguments[0],
            extensionReceiverTypeArgument = type.typeArguments[1],
            propertyTypeArgument = type.typeArguments[2]
        )
        is FirImplicitKMutableProperty2TypeRef -> FirImplicitKMutableProperty2TypeRef(
            newSource,
            dispatchReceiverTypeArgument = type.typeArguments[0],
            extensionReceiverTypeArgument = type.typeArguments[1],
            propertyTypeArgument = type.typeArguments[2]
        )
    }
}
