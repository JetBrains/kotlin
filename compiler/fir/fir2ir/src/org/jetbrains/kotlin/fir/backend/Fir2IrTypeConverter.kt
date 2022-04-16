/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.getAnnotationsByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ExtensionFunctionType
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance

class Fir2IrTypeConverter(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    internal val classIdToSymbolMap by lazy {
        mapOf(
            StandardClassIds.Nothing to irBuiltIns.nothingClass,
            StandardClassIds.Unit to irBuiltIns.unitClass,
            StandardClassIds.Boolean to irBuiltIns.booleanClass,
            StandardClassIds.String to irBuiltIns.stringClass,
            StandardClassIds.Any to irBuiltIns.anyClass,
            StandardClassIds.Long to irBuiltIns.longClass,
            StandardClassIds.Int to irBuiltIns.intClass,
            StandardClassIds.Short to irBuiltIns.shortClass,
            StandardClassIds.Byte to irBuiltIns.byteClass,
            StandardClassIds.Float to irBuiltIns.floatClass,
            StandardClassIds.Double to irBuiltIns.doubleClass,
            StandardClassIds.Char to irBuiltIns.charClass,
            StandardClassIds.Array to irBuiltIns.arrayClass
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

    private val typeApproximator = ConeTypeApproximator(session.typeContext, session.languageVersionSettings)

    private val typeApproximatorConfiguration =
        object : TypeApproximatorConfiguration.AllFlexibleSameValue() {
            override val allFlexible: Boolean get() = true
            override val errorType: Boolean get() = true
            override val integerLiteralConstantType: Boolean get() = true
            override val integerConstantOperatorType: Boolean get() = true
            override val intersectionTypesInContravariantPositions: Boolean get() = true
        }

    fun FirTypeRef.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType {
        capturedTypeCache.clear()
        return when (this) {
            !is FirResolvedTypeRef -> createErrorType()
            !is FirImplicitBuiltinTypeRef -> type.toIrType(typeContext, annotations)
            is FirImplicitNothingTypeRef -> irBuiltIns.nothingType
            is FirImplicitUnitTypeRef -> irBuiltIns.unitType
            is FirImplicitBooleanTypeRef -> irBuiltIns.booleanType
            is FirImplicitStringTypeRef -> irBuiltIns.stringType
            is FirImplicitAnyTypeRef -> irBuiltIns.anyType
            is FirImplicitIntTypeRef -> irBuiltIns.intType
            is FirImplicitNullableAnyTypeRef -> irBuiltIns.anyNType
            is FirImplicitNullableNothingTypeRef -> irBuiltIns.nothingNType
            else -> type.toIrType(typeContext, annotations)
        }
    }

    fun ConeKotlinType.toIrType(
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT,
        annotations: List<FirAnnotation> = emptyList(),
        hasFlexibleNullability: Boolean = false,
        addRawTypeAnnotation: Boolean = false
    ): IrType {
        return when (this) {
            is ConeErrorType -> createErrorType()
            is ConeLookupTagBasedType -> {
                val typeAnnotations = mutableListOf<IrConstructorCall>()
                typeAnnotations += with(annotationGenerator) { annotations.toIrAnnotations() }

                val irSymbol =
                    getBuiltInClassSymbol(classId)
                        ?: lookupTag.toSymbol(session)?.toSymbol(session, classifierStorage, typeContext) {
                            typeAnnotations += with(annotationGenerator) { it.toIrAnnotations() }
                        }
                        ?: (lookupTag as? ConeClassLikeLookupTag)?.let(classifierStorage::getIrClassSymbolForNotFoundClass)
                        ?: return createErrorType()

                when {
                    hasEnhancedNullability -> {
                        builtIns.enhancedNullabilityAnnotationConstructorCall()?.let {
                            typeAnnotations += it
                        }
                    }
                    hasFlexibleNullability -> {
                        builtIns.flexibleNullabilityAnnotationConstructorCall()?.let {
                            typeAnnotations += it
                        }
                    }
                }

                if (isExtensionFunctionType && annotations.getAnnotationsByClassId(ExtensionFunctionType).isEmpty()) {
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
                    if (annotations.any { it.classId == attributeAnnotation.classId }) continue
                    typeAnnotations += callGenerator.convertToIrConstructorCall(attributeAnnotation) as? IrConstructorCall ?: continue
                }
                val expandedType = fullyExpandedType(session)
                val approximatedType = approximateType(expandedType)
                IrSimpleTypeImpl(
                    irSymbol,
                    hasQuestionMark = !typeContext.definitelyNotNull && approximatedType.isMarkedNullable,
                    arguments = approximatedType.typeArguments.map { it.toIrTypeArgument(typeContext) },
                    annotations = typeAnnotations
                )
            }
            is ConeRawType -> {
                // Upper bound has star projections here, so we take lower one
                // (some reflection tests rely on this)
                lowerBound.toIrType(
                    typeContext,
                    hasFlexibleNullability = lowerBound.nullability != upperBound.nullability,
                    addRawTypeAnnotation = true
                )
            }
            is ConeFlexibleType -> {
                // TODO: yet we take more general type. Not quite sure it's Ok
                upperBound.toIrType(typeContext, hasFlexibleNullability = lowerBound.nullability != upperBound.nullability)
            }
            is ConeCapturedType -> {
                val cached = capturedTypeCache[this]
                if (cached == null) {
                    capturedTypeCache[this] = errorTypeForCapturedTypeStub
                    val supertypes = constructor.supertypes!!
                    val approximation = supertypes.find {
                        it == (constructor.projection as? ConeKotlinTypeProjection)?.type
                    } ?: supertypes.first()
                    val irType = approximation.toIrType(typeContext)
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
                original.toIrType(typeContext).makeNotNull()
            }
            is ConeIntersectionType -> {
                // TODO: add intersectionTypeApproximation
                intersectedTypes.first().toIrType(typeContext)
            }
            is ConeStubType -> createErrorType()
            is ConeIntegerLiteralType -> createErrorType()
        }
    }

    private fun ConeTypeProjection.toIrTypeArgument(typeContext: ConversionTypeContext): IrTypeArgument {
        fun toIrTypeArgument(type: ConeKotlinType, variance: Variance): IrTypeProjection {
            val irType = type.toIrType(typeContext)
            return makeTypeProjection(irType, if (typeContext.invariantProjection) Variance.INVARIANT else variance)
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
                    val irType = toIrType(typeContext)
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
        return substitutor.substituteOrSelf(type).let {
            typeApproximator.approximateToSuperType(it, typeApproximatorConfiguration) ?: it
        }
    }
}

fun FirTypeRef.toIrType(
    typeConverter: Fir2IrTypeConverter,
    typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
): IrType =
    with(typeConverter) {
        toIrType(typeContext)
    }
