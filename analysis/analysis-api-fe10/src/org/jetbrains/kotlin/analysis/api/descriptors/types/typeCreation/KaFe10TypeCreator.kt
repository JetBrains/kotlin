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
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.intersectTypes
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFe10TypeCreator(
    analysisSession: KaFe10Session
) : KaBaseTypeCreator<KaFe10Session>(analysisSession) {
    override fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId)
        val builder = KaBaseClassTypeBuilder(this).apply(init)

        if (descriptor == null) {
            val name = classId.asString()
            return buildClassErrorType(name, builder)
        }

        return buildClassType(descriptor, builder)
    }

    override fun classType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val descriptor = getSymbolDescriptor(symbol) as? ClassDescriptor
        val builder = KaBaseClassTypeBuilder(this).apply(init)

        if (descriptor == null) {
            val name = symbol.classId?.asString() ?: symbol.nameOrAnonymous.asString()
            return buildClassErrorType(name, builder)
        }

        return buildClassType(descriptor, builder)
    }

    private fun buildClassErrorType(name: String, builder: KaBaseClassTypeBuilder): KaClassErrorType {
        val kotlinType =
            ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, name)
                .withAnnotations(builder.annotations) as ErrorType

        return KaFe10ClassErrorType(kotlinType, analysisContext)
    }

    private fun buildClassType(descriptor: ClassDescriptor, builder: KaBaseClassTypeBuilder): KaClassType {
        val typeParameters = descriptor.typeConstructor.parameters
        val providedTypeArguments = builder.typeArguments
        val projections = typeParameters.mapIndexed { index, typeParameter ->
            when (val argument = providedTypeArguments.getOrNull(index)) {
                is KaStarTypeProjection, null -> StarProjectionImpl(typeParameter)
                is KaTypeArgumentWithVariance -> TypeProjectionImpl(argument.variance, (argument.type as KaFe10Type).fe10Type)
            }
        }

        val type = TypeUtils.substituteProjectionsForParameters(descriptor, projections).withAnnotations(builder.annotations)


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
            return typeWithNullability.withAnnotations(builder.annotations).toKtType(analysisContext) as KaTypeParameterType
        }

    override fun capturedType(
        type: KaCapturedType,
        init: KaCapturedTypeBuilder.() -> Unit
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaBaseCapturedTypeBuilder(this).apply(init)
            val projection = type.projection
            return buildCapturedType(projection, builder)
        }
    }

    override fun capturedType(
        projection: KaTypeProjection,
        init: KaCapturedTypeBuilder.() -> Unit,
    ): KaCapturedType {
        withValidityAssertion {
            val builder = KaBaseCapturedTypeBuilder(this).apply(init)
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

            val kotlinType = CapturedType(
                typeProjection = projection,
                isMarkedNullable = builder.isMarkedNullable,
                attributes = constructTypeAttributes(builder.annotations)
            )
            return kotlinType.toKtType(analysisContext) as KaCapturedType
        }
    }

    override fun definitelyNotNullType(
        type: KaTypeParameterType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit
    ): KaType = withValidityAssertion {
        buildDefinitelyNotNullType(type, KaBaseDefinitelyNotNullTypeBuilder(this).apply(init))
    }

    override fun definitelyNotNullType(
        type: KaCapturedType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit
    ): KaType = withValidityAssertion {
        buildDefinitelyNotNullType(type, KaBaseDefinitelyNotNullTypeBuilder(this).apply(init))
    }

    private fun buildDefinitelyNotNullType(type: KaType, builder: KaDefinitelyNotNullTypeBuilder): KaType {
        with(analysisSession) {
            require(type is KaFe10Type)
            return DefinitelyNotNullType.makeDefinitelyNotNull(type.fe10Type)
                ?.withAnnotations(builder.annotations)
                ?.toKtType(analysisContext) ?: type
        }
    }

    override fun flexibleType(
        type: KaFlexibleType,
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaType? {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder(
                type.lowerBound,
                type.upperBound,
                this
            ).apply(init)
            return buildFlexibleType(builder)
        }
    }

    override fun flexibleType(
        init: KaFlexibleTypeBuilder.() -> Unit,
    ): KaType? {
        withValidityAssertion {
            with(analysisSession) {
                val builder = KaBaseFlexibleTypeBuilder(
                    lowerBound = builtinTypes.nothing,
                    upperBound = builtinTypes.nullableAny,
                    this@KaFe10TypeCreator
                ).apply(init)
                return buildFlexibleType(builder)
            }
        }
    }

    override fun flexibleType(
        lowerBound: KaType,
        upperBound: KaType,
    ): KaType? {
        withValidityAssertion {
            val builder = KaBaseFlexibleTypeBuilder(
                lowerBound,
                upperBound,
                this
            )
            return buildFlexibleType(builder)
        }
    }

    private fun buildFlexibleType(builder: KaFlexibleTypeBuilder): KaType? {
        with(analysisSession) {
            val lowerBound = builder.lowerBound.lowerBoundIfFlexible()
            require(lowerBound is KaFe10Type)
            val upperBound = builder.upperBound.upperBoundIfFlexible()
            require(upperBound is KaFe10Type)


            if (lowerBound == upperBound) {
                return lowerBound.fe10Type.withAnnotations(builder.annotations).toKtType(analysisContext)
            }

            if (!lowerBound.isSubtypeOf(upperBound)) {
                return null
            }

            val kotlinType = FlexibleTypeImpl(lowerBound.fe10Type.asSimpleType(), upperBound.fe10Type.asSimpleType())
            return kotlinType.withAnnotations(builder.annotations).toKtType(analysisContext) as KaFlexibleType
        }
    }

    override fun intersectionType(
        init: KaIntersectionTypeBuilder.() -> Unit,
    ): KaType {
        withValidityAssertion {
            val builder = KaBaseIntersectionTypeBuilder(this).apply(init)
            val conjuncts = builder.conjuncts.map { conjunctType -> (conjunctType as KaFe10Type).fe10Type }

            if (conjuncts.isEmpty()) {
                val nullableAny = analysisSession.builtinTypes.nullableAny
                return (nullableAny as KaFe10Type).fe10Type.toKtType(analysisContext)
            }

            return intersectTypes(conjuncts).toKtType(analysisContext)
        }
    }

    override fun dynamicType(init: KaDynamicTypeBuilder.() -> Unit): KaDynamicType {
        withValidityAssertion {
            val builder = KaBaseDynamicTypeBuilder(this).apply(init)
            val kotlinType = createDynamicType(analysisContext.builtIns)
            return kotlinType.withAnnotations(builder.annotations).toKtType(analysisContext) as KaDynamicType
        }
    }

    private fun KotlinType.withAnnotations(annotationClassIds: List<ClassId>): KotlinType {
        if (annotationClassIds.isEmpty()) {
            return this
        }

        return replaceAnnotations(constructAnnotations(annotationClassIds))
    }

    private fun constructTypeAttributes(annotationClassIds: List<ClassId>): TypeAttributes {
        val annotations = constructAnnotations(annotationClassIds)
        val annotationsTypeAttribute = AnnotationsTypeAttribute(annotations)

        return TypeAttributes.create(listOf(annotationsTypeAttribute))
    }

    private fun constructAnnotations(annotationClassIds: List<ClassId>): Annotations = Annotations.create(
        annotationClassIds.mapNotNull(::constructAnnotationDescriptor)
    )

    private fun constructAnnotationDescriptor(annotationClassId: ClassId): AnnotationDescriptor? {
        val descriptor: ClassDescriptor =
            analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(annotationClassId) ?: return null

        if (!descriptor.kind.isAnnotationClass) {
            return null
        }

        if (descriptor.unsubstitutedPrimaryConstructor?.valueParameters?.isEmpty() != true) {
            return null
        }

        return AnnotationDescriptorImpl(
            descriptor.defaultType,
            emptyMap(),
            SourceElement.NO_SOURCE
        )
    }

    private val analysisContext: Fe10AnalysisContext
        get() = analysisSession.analysisContext
}