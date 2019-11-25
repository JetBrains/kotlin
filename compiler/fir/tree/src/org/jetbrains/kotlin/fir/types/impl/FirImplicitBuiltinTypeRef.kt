/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId

sealed class FirImplicitBuiltinTypeRef(
    override val source: FirSourceElement?,
    val id: ClassId,
    typeArguments: Array<out ConeKotlinTypeProjection> = emptyArray(),
    isNullable: Boolean = false
) : FirResolvedTypeRef() {
    override val annotations: List<FirAnnotationCall>
        get() = emptyList()

    override val type: ConeClassLikeType = ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(id), typeArguments, isNullable)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}

class FirImplicitUnitTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Unit)

class FirImplicitAnyTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Any)

class FirImplicitNullableAnyTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Any, isNullable = true)

class FirImplicitEnumTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Enum)

class FirImplicitAnnotationTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Annotation)

class FirImplicitBooleanTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Boolean)

class FirImplicitNothingTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Nothing)

class FirImplicitNullableNothingTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.Nothing, isNullable = true)

class FirImplicitStringTypeRef(
    source: FirSourceElement?
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.String)

class FirImplicitKPropertyTypeRef(
    source: FirSourceElement?,
    typeArgument: ConeKotlinTypeProjection
) : FirImplicitBuiltinTypeRef(source, StandardClassIds.KProperty, arrayOf(typeArgument))
