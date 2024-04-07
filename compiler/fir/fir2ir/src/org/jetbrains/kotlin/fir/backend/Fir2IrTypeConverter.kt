/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.getAnnotationsByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.unexpandedConeClassLikeType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ExtensionFunctionType
import org.jetbrains.kotlin.types.CommonFlexibleTypeBoundsChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance

class Fir2IrTypeConverter(
    private val c: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope,
) : Fir2IrComponents by c {

    internal val classIdToSymbolMap by lazy {
        // Note: this map must include all base classes, and they should be before derived classes!
        mapOf(
            StandardClassIds.Nothing to irBuiltIns.nothingClass,
            StandardClassIds.Any to irBuiltIns.anyClass,
            StandardClassIds.Unit to irBuiltIns.unitClass,
            StandardClassIds.Boolean to irBuiltIns.booleanClass,
            StandardClassIds.CharSequence to irBuiltIns.charSequenceClass,
            StandardClassIds.String to irBuiltIns.stringClass,
            StandardClassIds.Number to irBuiltIns.numberClass,
            StandardClassIds.Long to irBuiltIns.longClass,
            StandardClassIds.Int to irBuiltIns.intClass,
            StandardClassIds.Short to irBuiltIns.shortClass,
            StandardClassIds.Byte to irBuiltIns.byteClass,
            StandardClassIds.Float to irBuiltIns.floatClass,
            StandardClassIds.Double to irBuiltIns.doubleClass,
            StandardClassIds.Char to irBuiltIns.charClass,
            StandardClassIds.Array to irBuiltIns.arrayClass,
            StandardClassIds.Annotations.IntrinsicConstEvaluation to irBuiltIns.intrinsicConstSymbol
        )
    }

    internal val classIdToTypeMap by lazy {
        mapOf(
            StandardClassIds.Nothing to irBuiltIns.nothingType,
            StandardClassIds.Unit to irBuiltIns.unitType,
            StandardClassIds.Boolean to irBuiltIns.booleanType,
            StandardClassIds.String to irBuiltIns.stringType,
            StandardClassIds.Any to irBuiltIns.anyType,
            StandardClassIds.Long to irBuiltIns.longType,
            StandardClassIds.Int to irBuiltIns.intType,
            StandardClassIds.Short to irBuiltIns.shortType,
            StandardClassIds.Byte to irBuiltIns.byteType,
            StandardClassIds.Float to irBuiltIns.floatType,
            StandardClassIds.Double to irBuiltIns.doubleType,
            StandardClassIds.Char to irBuiltIns.charType
        )
    }

    private val capturedTypeCache = mutableMapOf<ConeCapturedType, IrType>()
    private val errorTypeForCapturedTypeStub by lazy { createErrorType() }

    fun FirTypeRef.toIrType(typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType {
        capturedTypeCache.clear()
        return when (this) {
            !is FirResolvedTypeRef -> createErrorType()
            !is FirImplicitBuiltinTypeRef -> type.toIrType(typeOrigin, annotations)
            is FirImplicitNothingTypeRef -> irBuiltIns.nothingType
            is FirImplicitUnitTypeRef -> irBuiltIns.unitType
            is FirImplicitBooleanTypeRef -> irBuiltIns.booleanType
            is FirImplicitStringTypeRef -> irBuiltIns.stringType
            is FirImplicitAnyTypeRef -> irBuiltIns.anyType
            is FirImplicitIntTypeRef -> irBuiltIns.intType
            is FirImplicitNullableAnyTypeRef -> irBuiltIns.anyNType
            is FirImplicitNullableNothingTypeRef -> irBuiltIns.nothingNType
            else -> type.toIrType(typeOrigin, annotations)
        }
    }

    fun ConeKotlinType.toIrType(
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
        annotations: List<FirAnnotation> = emptyList(),
        hasFlexibleNullability: Boolean = false,
        hasFlexibleMutability: Boolean = false,
        hasFlexibleArrayElementVariance: Boolean = false,
        addRawTypeAnnotation: Boolean = false
    ): IrType {
        return when (this) {
            is ConeErrorType -> createErrorType()
            is ConeLookupTagBasedType -> {
                val typeAnnotations = mutableListOf<IrConstructorCall>()
                typeAnnotations += with(annotationGenerator) { annotations.toIrAnnotations() }

                val irSymbol =
                    getBuiltInClassSymbol(classId)
                        ?: lookupTag.toSymbol(session)?.toSymbol(c, typeOrigin) {
                            typeAnnotations += with(annotationGenerator) { it.toIrAnnotations() }
                        }
                        ?: (lookupTag as? ConeClassLikeLookupTag)?.let(classifiersGenerator::createIrClassForNotFoundClass)?.symbol
                        ?: return createErrorType()

                if (hasEnhancedNullability) {
                    builtIns.enhancedNullabilityAnnotationConstructorCall()?.let {
                        typeAnnotations += it
                    }
                }
                if (hasFlexibleNullability) {
                    builtIns.flexibleNullabilityAnnotationConstructorCall()?.let {
                        typeAnnotations += it
                    }
                }
                if (hasFlexibleMutability) {
                    builtIns.flexibleMutabilityAnnotationConstructorCall()?.let {
                        typeAnnotations += it
                    }
                }
                if (hasFlexibleArrayElementVariance) {
                    builtIns.flexibleArrayElementVarianceAnnotationConstructorCall()?.let {
                        typeAnnotations += it
                    }
                }

                if (isExtensionFunctionType && annotations.getAnnotationsByClassId(ExtensionFunctionType, session).isEmpty()) {
                    builtIns.extensionFunctionTypeAnnotationConstructorCall()?.let {
                        typeAnnotations += it
                    }
                }

                if (addRawTypeAnnotation) {
                    builtIns.rawTypeAnnotationConstructorCall()?.let {
                        typeAnnotations += it
                    }
                }

                for (attributeAnnotation in attributes.customAnnotations) {
                    val isAlreadyPresentInAnnotations = annotations.any {
                        it.unexpandedConeClassLikeType == attributeAnnotation.unexpandedConeClassLikeType
                    }
                    if (isAlreadyPresentInAnnotations) continue
                    typeAnnotations += callGenerator.convertToIrConstructorCall(attributeAnnotation) as? IrConstructorCall ?: continue
                }
                val expandedType = fullyExpandedType(session)
                val approximatedType = approximateType(expandedType)

                if (approximatedType is ConeTypeParameterType && conversionScope.shouldEraseType(approximatedType)) {
                    // This hack is about type parameter leak in case of generic delegated property
                    // It probably will be prohibited after 2.0
                    // For more details see KT-24643
                    return approximateUpperBounds(approximatedType.lookupTag.typeParameterSymbol.resolvedBounds)
                }

                IrSimpleTypeImpl(
                    irSymbol,
                    hasQuestionMark = approximatedType.isMarkedNullable,
                    arguments = approximatedType.typeArguments.map { it.toIrTypeArgument(typeOrigin) },
                    annotations = typeAnnotations
                )
            }
            is ConeRawType -> {
                // Upper bound has star projections here, so we take lower one
                // (some reflection tests rely on this)
                lowerBound.withNullability(upperBound.nullability, session.typeContext).toIrType(
                    typeOrigin,
                    annotations,
                    hasFlexibleNullability = lowerBound.nullability != upperBound.nullability,
                    hasFlexibleMutability = isMutabilityFlexible(),
                    hasFlexibleArrayElementVariance = false,
                    addRawTypeAnnotation = true
                )
            }
            is ConeDynamicType -> {
                val typeAnnotations = with(annotationGenerator) { annotations.toIrAnnotations() }
                return IrDynamicTypeImpl(null, typeAnnotations, Variance.INVARIANT)
            }
            is ConeFlexibleType -> with(session.typeContext) {
                if (upperBound is ConeClassLikeType) {
                    val upper = upperBound as ConeClassLikeType
                    val lower = lowerBound
                    val intermediate = if (lower is ConeClassLikeType && lower.lookupTag == upper.lookupTag) {
                        lower.replaceArguments(upper.getArguments())
                    } else lower
                    (intermediate.withNullability(upper.isNullable) as ConeKotlinType)
                        .withAttributes(lower.attributes)
                        .toIrType(
                            typeOrigin,
                            annotations,
                            hasFlexibleNullability = lower.nullability != upper.nullability,
                            hasFlexibleMutability = isMutabilityFlexible(),
                            hasFlexibleArrayElementVariance = hasFlexibleArrayElementVariance(),
                        )
                } else {
                    upperBound.toIrType(
                        typeOrigin,
                        annotations,
                        hasFlexibleNullability = lowerBound.nullability != upperBound.nullability,
                        hasFlexibleMutability = isMutabilityFlexible(),
                        hasFlexibleArrayElementVariance = false,
                    )
                }
            }
            is ConeCapturedType -> {
                val cached = capturedTypeCache[this]
                if (cached == null) {
                    capturedTypeCache[this] = errorTypeForCapturedTypeStub
                    val irType = this.approximateForIrOrSelf(c).toIrType(
                        typeOrigin,
                        annotations,
                        hasFlexibleNullability = hasFlexibleNullability,
                        hasFlexibleMutability = hasFlexibleMutability,
                        addRawTypeAnnotation = addRawTypeAnnotation
                    )
                    capturedTypeCache[this] = irType
                    irType
                } else {
                    // Potentially recursive captured type, e.g., Recursive<R> where R : Recursive<R>, ...
                    // That should have been handled during type argument conversion, though.
                    // Or, simply repeated captured type, e.g., FunctionN<..., *, ..., *>, literally same captured types.
                    cached
                }
            }
            is ConeDefinitelyNotNullType -> {
                original.toIrType(c, typeOrigin).makeNotNull()
            }
            is ConeIntersectionType -> {
                val approximated = approximateForIrOrNull(c)!!
                approximated.toIrType(c, typeOrigin)
            }
            is ConeStubType, is ConeIntegerLiteralType, is ConeTypeVariableType -> createErrorType()
        }
    }

    private fun ConeFlexibleType.hasFlexibleArrayElementVariance(): Boolean =
        lowerBound.let { lowerBound ->
            lowerBound is ConeClassLikeType && lowerBound.lookupTag.classId == StandardClassIds.Array &&
                    lowerBound.typeArguments.single().kind == ProjectionKind.INVARIANT
        } && upperBound.let { upperBound ->
            upperBound is ConeClassLikeType && upperBound.lookupTag.classId == StandardClassIds.Array &&
                    upperBound.typeArguments.single().kind == ProjectionKind.OUT
        }

    private fun approximateUpperBounds(resolvedBounds: List<FirResolvedTypeRef>): IrType {
        val commonSupertype = session.typeContext.commonSuperTypeOrNull(resolvedBounds.map { it.type })!!
        val resultType = (commonSupertype as? ConeClassLikeType)?.replaceArgumentsWithStarProjections()
            ?: commonSupertype
        val approximatedType = (commonSupertype as? ConeSimpleKotlinType)?.let { approximateType(it) } ?: resultType
        return approximatedType.toIrType(c)
    }

    private fun ConeFlexibleType.isMutabilityFlexible(): Boolean {
        val lowerFqName = lowerBound.classId?.asSingleFqName() ?: return false
        val upperFqName = upperBound.classId?.asSingleFqName() ?: return false
        if (lowerFqName == upperFqName) return false
        return CommonFlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(lowerFqName) ==
                CommonFlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(upperFqName)
    }

    private fun ConeTypeProjection.toIrTypeArgument(typeOrigin: ConversionTypeOrigin): IrTypeArgument {
        fun toIrTypeArgument(type: ConeKotlinType, variance: Variance): IrTypeProjection {
            val irType = type.toIrType(c, typeOrigin)
            return makeTypeProjection(irType, variance)
        }

        return when (this) {
            ConeStarProjection -> IrStarProjectionImpl
            is ConeKotlinTypeProjectionIn -> toIrTypeArgument(this.type, Variance.IN_VARIANCE)
            is ConeKotlinTypeProjectionOut -> toIrTypeArgument(this.type, Variance.OUT_VARIANCE)
            is ConeKotlinTypeConflictingProjection -> toIrTypeArgument(this.type, Variance.INVARIANT)
            is ConeKotlinType -> {
                if (this is ConeCapturedType && this in capturedTypeCache && this.isRecursive(mutableSetOf())) {
                    // Recursive captured type, e.g., Recursive<R> where R : Recursive<R>, ...
                    // We can return * early here to avoid recursive type conversions.
                    IrStarProjectionImpl
                } else {
                    val irType = toIrType(c, typeOrigin)
                    makeTypeProjection(irType, Variance.INVARIANT)
                }
            }
        }
    }

    private fun ConeKotlinType.isRecursive(visited: MutableSet<ConeCapturedType>): Boolean =
        when (this) {
            is ConeLookupTagBasedType -> {
                typeArguments.any {
                    when (it) {
                        is ConeKotlinType -> it.isRecursive(visited)
                        is ConeKotlinTypeProjectionIn -> it.type.isRecursive(visited)
                        is ConeKotlinTypeProjectionOut -> it.type.isRecursive(visited)
                        else -> false
                    }
                }
            }
            is ConeFlexibleType -> {
                lowerBound.isRecursive(visited) || upperBound.isRecursive(visited)
            }
            is ConeCapturedType -> {
                if (visited.add(this)) {
                    constructor.supertypes?.any { it.isRecursive(visited) } == true
                } else
                    true
            }
            is ConeDefinitelyNotNullType -> {
                original.isRecursive(visited)
            }
            is ConeIntersectionType -> {
                intersectedTypes.any { it.isRecursive(visited) }
            }
            else -> false
        }

    private fun getArrayClassSymbol(classId: ClassId?): IrClassSymbol? {
        val primitiveId = StandardClassIds.elementTypeByPrimitiveArrayType[classId] ?: return null
        val irType = classIdToTypeMap[primitiveId]
        return irBuiltIns.primitiveArrayForType[irType] ?: error("Strange primitiveId $primitiveId from array: $classId")
    }

    private fun getBuiltInClassSymbol(classId: ClassId?): IrClassSymbol? {
        return classIdToSymbolMap[classId] ?: getArrayClassSymbol(classId)
    }

    private fun approximateType(type: ConeSimpleKotlinType): ConeKotlinType {
        if (type is ConeClassLikeType && type.typeArguments.isEmpty()) return type
        val substitutor = object : AbstractConeSubstitutor(session.typeContext) {
            override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
                return if (type is ConeIntersectionType) {
                    type.alternativeType?.let { substituteOrSelf(it) }
                } else null
            }
        }
        return substitutor.substituteOrSelf(type).approximateForIrOrSelf(c)
    }
}

fun FirTypeRef.toIrType(
    typeConverter: Fir2IrTypeConverter,
    typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
): IrType {
    return with(typeConverter) {
        toIrType(typeOrigin)
    }
}

fun FirTypeRef.toIrType(c: Fir2IrComponents, typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType {
    return with(c.typeConverter) {
        toIrType(typeOrigin)
    }
}

fun ConeKotlinType.toIrType(c: Fir2IrComponents, typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType {
    return with(c.typeConverter) {
        toIrType(typeOrigin)
    }
}

internal fun ConeKotlinType.approximateForIrOrNull(c: Fir2IrComponents): ConeKotlinType? {
    return c.session.typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.FrontendToBackendTypesApproximation)
}

internal fun ConeKotlinType.approximateForIrOrSelf(c: Fir2IrComponents): ConeKotlinType {
    return approximateForIrOrNull(c) ?: this
}
