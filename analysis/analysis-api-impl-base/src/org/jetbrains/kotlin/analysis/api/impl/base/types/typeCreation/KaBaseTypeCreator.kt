/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
abstract class KaBaseTypeCreator<T : KaSession>(val analysisSession: T) : KaTypeCreator {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun typeProjection(
        variance: Variance,
        type: KaType,
    ): KaTypeArgumentWithVariance = withValidityAssertion {
        KaBaseTypeArgumentWithVariance(type, variance, token)
    }

    override fun typeProjection(
        variance: Variance,
        type: KaTypeCreator.() -> KaType,
    ): KaTypeArgumentWithVariance = withValidityAssertion {
        KaBaseTypeArgumentWithVariance(type(), variance, token)
    }

    override fun starTypeProjection(): KaStarTypeProjection = withValidityAssertion {
        KaBaseStarTypeProjection(token)
    }

    override fun arrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        with(analysisSession) {
            val builder = KaBaseArrayTypeBuilder(this@KaBaseTypeCreator).apply(init)

            if (elementType is KaClassType && builder.shouldPreferPrimitiveTypes && !elementType.isMarkedNullable) {
                val classId = elementType.classId
                val primitiveArrayId =
                    StandardClassIds.primitiveArrayTypeByElementType[classId]
                        ?: StandardClassIds.unsignedArrayTypeByElementType[classId]
                if (primitiveArrayId != null) {
                    return classType(primitiveArrayId) {
                        isMarkedNullable = builder.isMarkedNullable
                        annotations(builder.annotations)
                    }
                }
            }

            return classType(StandardClassIds.Array) {
                isMarkedNullable = builder.isMarkedNullable
                typeArgument(builder.variance, elementType)
                annotations(builder.annotations)
            }
        }
    }

    override fun varargArrayType(elementType: KaType): KaType = withValidityAssertion {
        arrayType(elementType) {
            variance = Variance.OUT_VARIANCE
        }
    }
}

@KaImplementationDetail
abstract class KaBaseTypeBuilderWithAnnotations : KaTypeBuilderWithAnnotations {
    val backingAnnotations = mutableListOf<ClassId>()

    override val annotations: List<ClassId>
        get() = withValidityAssertion {
            backingAnnotations
        }

    override fun annotation(annotationClassId: ClassId) = withValidityAssertion {
        backingAnnotations += annotationClassId
    }

    override fun annotation(annotationClassId: () -> ClassId) = withValidityAssertion {
        backingAnnotations += annotationClassId()
    }

    override fun annotations(annotationClassIds: Iterable<ClassId>) = withValidityAssertion {
        backingAnnotations += annotationClassIds
    }

    override fun annotations(annotationClassIds: () -> Iterable<ClassId>) = withValidityAssertion {
        backingAnnotations += annotationClassIds()
    }
}

@KaImplementationDetail
class KaBaseClassTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaClassTypeBuilder, KaBaseTypeBuilderWithAnnotations(),
    KaTypeCreator by typeCreatorDelegate {
    private val backingTypeArguments = mutableListOf<KaTypeProjection>()

    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    override val typeArguments: List<KaTypeProjection>
        get() = withValidityAssertion { backingTypeArguments }

    override fun typeArgument(argument: KaTypeProjection) = withValidityAssertion {
        backingTypeArguments += argument
    }

    override fun invariantTypeArgument(type: () -> KaType) = invariantTypeArgument(type())

    override fun invariantTypeArgument(type: KaType) = typeArgument(Variance.INVARIANT, type)

    override fun typeArgument(variance: Variance, type: () -> KaType) = typeArgument(variance, type())

    override fun typeArgument(variance: Variance, type: KaType) = withValidityAssertion {
        backingTypeArguments += KaBaseTypeArgumentWithVariance(type, variance, type.token)
    }

    override fun typeArguments(arguments: () -> Iterable<KaTypeProjection>) = withValidityAssertion {
        backingTypeArguments += arguments()
    }

    override fun typeArgument(argument: () -> KaTypeProjection) = typeArgument(argument())
}

@KaImplementationDetail
class KaBaseTypeParameterTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaTypeParameterTypeBuilder, KaBaseTypeBuilderWithAnnotations(),
    KaTypeCreator by typeCreatorDelegate {
    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }
}

@KaImplementationDetail
class KaBaseArrayTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaArrayTypeBuilder, KaBaseTypeBuilderWithAnnotations(),
    KaTypeCreator by typeCreatorDelegate {
    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    override var shouldPreferPrimitiveTypes: Boolean = true
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    override var variance: Variance = Variance.INVARIANT
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }
}

@KaImplementationDetail
class KaBaseCapturedTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaCapturedTypeBuilder, KaBaseTypeBuilderWithAnnotations(),
    KaTypeCreator by typeCreatorDelegate {
    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }
}

@KaImplementationDetail
class KaBaseDefinitelyNotNullTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaDefinitelyNotNullTypeBuilder,
    KaBaseTypeBuilderWithAnnotations(),
    KaTypeCreator by typeCreatorDelegate

@KaImplementationDetail
class KaBaseFlexibleTypeBuilder(lowerBound: KaType, upperBound: KaType, typeCreatorDelegate: KaTypeCreator) : KaFlexibleTypeBuilder,
    KaBaseTypeBuilderWithAnnotations(), KaTypeCreator by typeCreatorDelegate {
    override var lowerBound: KaType = lowerBound
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }

    override var upperBound: KaType = upperBound
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }
}

@KaImplementationDetail
class KaBaseIntersectionTypeBuilder(
    typeCreatorDelegate: KaTypeCreator
) : KaIntersectionTypeBuilder, KaTypeCreator by typeCreatorDelegate {

    private val backingConjuncts: MutableSet<KaType> = mutableSetOf()

    private fun KaType.unwrapConjunct(): List<KaType> = (this as? KaIntersectionType)?.conjuncts ?: listOf(this)

    override val conjuncts: Set<KaType> get() = withValidityAssertion { backingConjuncts }

    override fun conjunct(conjunct: KaType): Unit = withValidityAssertion {
        backingConjuncts += conjunct.unwrapConjunct()
    }

    override fun conjunct(conjunct: () -> KaType) = conjunct(conjunct())

    override fun conjuncts(conjuncts: () -> Iterable<KaType>) = conjuncts(conjuncts())

    override fun conjuncts(conjuncts: Iterable<KaType>) = withValidityAssertion {
        backingConjuncts += conjuncts.flatMap { it.unwrapConjunct() }
    }
}

@KaImplementationDetail
class KaBaseDynamicTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaDynamicTypeBuilder,
    KaBaseTypeBuilderWithAnnotations(), KaTypeCreator by typeCreatorDelegate

@KaImplementationDetail
class KaBaseFunctionTypeBuilder(typeCreatorDelegate: KaTypeCreator, session: KaSession) : KaFunctionTypeBuilder,
    KaBaseTypeBuilderWithAnnotations(),
    KaTypeCreator by typeCreatorDelegate {
    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    override var isSuspend: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    override var isReflectType: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    private val backingContextParameters: MutableList<KaType> = mutableListOf()

    override val contextParameters: List<KaType>
        get() = withValidityAssertion { backingContextParameters }

    override fun contextParameter(contextParameter: KaType) = withValidityAssertion {
        backingContextParameters += contextParameter
    }

    override fun contextParameter(contextParameter: () -> KaType) = contextParameter(contextParameter())

    private val backingValueParameters: MutableList<KaFunctionValueParameter> = mutableListOf()

    override val valueParameters: List<KaFunctionValueParameter>
        get() = withValidityAssertion { backingValueParameters }

    override fun valueParameter(name: Name?, type: KaType) = withValidityAssertion {
        val valueParameter = KaBaseFunctionValueParameter(name, type)
        backingValueParameters += valueParameter
    }

    override fun valueParameter(name: Name?, type: () -> KaType) = valueParameter(name, type())

    override var receiverType: KaType? = null
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }

    override var returnType: KaType = session.builtinTypes.unit
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }
}
