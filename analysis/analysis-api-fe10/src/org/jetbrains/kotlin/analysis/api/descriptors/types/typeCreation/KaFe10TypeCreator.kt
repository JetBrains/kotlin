/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types.typeCreation

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KaFe10ClassErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KaFe10UsualClassType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFe10TypeCreator(
    analysisSession: KaFe10Session
) : KaBaseTypeCreator<KaFe10Session>(analysisSession) {
    override fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.ByClassId(classId, this).apply(init))
    }

    override fun classType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.BySymbol(symbol, this).apply(init))
    }

    private fun buildClassType(builder: KaBaseClassTypeBuilder): KaType {
        val descriptor: ClassDescriptor? = when (builder) {
            is KaBaseClassTypeBuilder.ByClassId -> {
                analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(builder.classId)
            }
            is KaBaseClassTypeBuilder.BySymbol -> {
                getSymbolDescriptor(builder.symbol) as? ClassDescriptor
            }
        }

        if (descriptor == null) {
            val name = when (builder) {
                is KaBaseClassTypeBuilder.ByClassId -> builder.classId.asString()
                is KaBaseClassTypeBuilder.BySymbol ->
                    builder.symbol.classId?.asString()
                        ?: builder.symbol.name?.asString()
                        ?: SpecialNames.ANONYMOUS_STRING
            }
            val kotlinType = ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, name)
            return KaFe10ClassErrorType(kotlinType, analysisContext)
        }

        val typeParameters = descriptor.typeConstructor.parameters
        val providedTypeArguments = builder.typeArguments
        val projections = typeParameters.mapIndexed { index, typeParameter ->
            when (val argument = providedTypeArguments.getOrNull(index)) {
                is KaStarTypeProjection, null -> StarProjectionImpl(typeParameter)
                is KaTypeArgumentWithVariance -> TypeProjectionImpl(argument.variance, (argument.type as KaFe10Type).fe10Type)
            }
        }

        val type = TypeUtils.substituteProjectionsForParameters(descriptor, projections)


        val typeWithNullability = TypeUtils.makeNullableAsSpecified(type, builder.isMarkedNullable)
        return KaFe10UsualClassType(typeWithNullability as SimpleType, descriptor, analysisContext)
    }

    override fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder(this).apply(init)
            val descriptor = getSymbolDescriptor(symbol) as? TypeParameterDescriptor
            val kotlinType = descriptor?.defaultType
                ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_DESCRIPTOR_FOR_TYPE_PARAMETER, builder.toString())
            val typeWithNullability = TypeUtils.makeNullableAsSpecified(kotlinType, builder.isMarkedNullable)
            return typeWithNullability.toKtType(analysisContext) as KaTypeParameterType
        }

    override fun capturedType(
        type: KaCapturedType,
        init: KaCapturedTypeBuilder.() -> Unit
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaBaseCapturedTypeBuilder.Base(this).apply(init)
            val projection = type.projection
            return buildCapturedType(projection, builder)
        }
    }

    override fun capturedType(
        projection: KaTypeProjection,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaBaseCapturedTypeBuilder.Base(this).apply(init)
            return buildCapturedType(projection, builder)
        }
    }

    private fun buildCapturedType(originalProjection: KaTypeProjection, builder: KaCapturedTypeBuilder): KaCapturedType {
        with(analysisSession) {
            val projection = when (originalProjection) {
                is KaStarTypeProjection -> StarProjectionForAbsentTypeParameter(analysisContext.builtIns)
                is KaTypeArgumentWithVariance -> {
                    if (originalProjection.variance == Variance.INVARIANT) {
                        errorWithAttachment("Only non-invariant projections can be captured") {
                            withEntry("projection", Variance.INVARIANT.toString())
                            withEntry("type", originalProjection.type.render(position = Variance.INVARIANT))
                        }
                    }

                    TypeProjectionImpl(
                        originalProjection.variance,
                        (originalProjection.type as KaFe10Type).fe10Type
                    )
                }
            }

            val kotlinType = CapturedType(typeProjection = projection, isMarkedNullable = builder.isMarkedNullable)
            return kotlinType.toKtType(analysisContext) as KaCapturedType
        }
    }

    override fun definitelyNotNullType(type: KaType, init: KaDefinitelyNotNullTypeBuilder.() -> Unit): KaDefinitelyNotNullType {
        withValidityAssertion {
            with(analysisSession) {
                val builder = KaBaseDefinitelyNotNullTypeBuilder.Base(this@KaFe10TypeCreator).apply(init)
                require(type is KaFe10Type)

                if (type !is KaCapturedType && type !is KaTypeParameterType) {
                    errorWithAttachment("`KaDefinitelyNotNullType` can only wrap `KaCapturedType` or `KaTypeParameterType`") {
                        withEntry("type", type.render(position = Variance.INVARIANT))
                    }
                }

                return DefinitelyNotNullType.makeDefinitelyNotNull(type.fe10Type, avoidCheckingActualTypeNullability = true)
                    ?.toKtType(analysisContext) as KaDefinitelyNotNullType
            }
        }
    }

    override fun flexibleType(
        type: KaFlexibleType,
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaFlexibleType {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder.ByFlexibleType(type, this).apply(init)
            return buildFlexibleType(builder)
        }
    }

    override fun flexibleType(
        lowerBound: KaType,
        upperBound: KaType,
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaFlexibleType {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder.ByBounds(lowerBound, upperBound, this).apply(init)
            return buildFlexibleType(builder)
        }
    }

    private fun buildFlexibleType(builder: KaFlexibleTypeBuilder): KaFlexibleType {
        withValidityAssertion {
            with(analysisSession) {
                val lowerBound = builder.lowerBound.lowerBoundIfFlexible()
                require(lowerBound is KaFe10Type)
                val upperBound = builder.upperBound.upperBoundIfFlexible()
                require(upperBound is KaFe10Type)


                if (lowerBound == upperBound) {
                    errorWithAttachment("Lower and upper bounds are equal") {
                        withEntry("lowerBound", lowerBound.render(position = Variance.INVARIANT))
                        withEntry("upperBound", upperBound.render(position = Variance.INVARIANT))
                    }
                }

                if (!lowerBound.isSubtypeOf(upperBound)) {
                    errorWithAttachment("Lower bound must be a subtype of upper bound") {
                        withEntry("lowerBound", lowerBound.render(position = Variance.INVARIANT))
                        withEntry("upperBound", upperBound.render(position = Variance.INVARIANT))
                    }
                }

                val kotlinType = FlexibleTypeImpl(lowerBound.fe10Type.asSimpleType(), upperBound.fe10Type.asSimpleType())
                return kotlinType.toKtType(analysisContext) as KaFlexibleType
            }
        }
    }

    override fun intersectionType(
        type: KaIntersectionType,
        init: KaIntersectionTypeBuilder.() -> Unit,
    ): KaIntersectionType {
        withValidityAssertion {
            val builder = KaBaseIntersectionTypeBuilder.ByIntersectionType(type, this).apply(init)
            return buildIntersectionType(builder)
        }
    }

    override fun intersectionType(
        conjuncts: List<KaType>,
        init: KaIntersectionTypeBuilder.() -> Unit,
    ): KaIntersectionType {
        withValidityAssertion {
            val builder = KaBaseIntersectionTypeBuilder.ByConjuncts(conjuncts, this).apply(init)
            return buildIntersectionType(builder)
        }
    }

    private fun buildIntersectionType(builder: KaIntersectionTypeBuilder): KaIntersectionType {
        withValidityAssertion {
            val conjuncts = builder.conjuncts
            assert(conjuncts.isNotEmpty()) { "Intersection type must have at least one conjunct" }

            val intersectionConstructor = IntersectionTypeConstructor(
                typesToIntersect = conjuncts.map { conjunctType -> (conjunctType as KaFe10Type).fe10Type }
            )
            val simpleType = KotlinTypeFactory.simpleType(
                attributes = TypeAttributes.Empty,
                constructor = intersectionConstructor,
                arguments = emptyList(),
                nullable = false
            )

            return simpleType.toKtType(analysisContext) as KaIntersectionType
        }
    }

    override fun dynamicType(): KaDynamicType {
        withValidityAssertion {
            val kotlinType = createDynamicType(analysisContext.builtIns)
            return kotlinType.toKtType(analysisContext) as KaDynamicType
        }
    }

    private val analysisContext: Fe10AnalysisContext
        get() = analysisSession.analysisContext

}