/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
abstract class KaBaseTypeCreator<T : KaSession> : KaBaseSessionComponent<T>(), KaTypeCreator {
    override fun typeArgumentWithVariance(
        variance: Variance,
        type: KaType,
    ): KaTypeArgumentWithVariance = withValidityAssertion {
        KaBaseTypeArgumentWithVariance(type, variance, token)
    }

    override fun typeArgumentWithVariance(
        variance: Variance,
        type: KaTypeCreator.() -> KaType,
    ): KaTypeArgumentWithVariance = withValidityAssertion {
        KaBaseTypeArgumentWithVariance(type(), variance, token)
    }

    override fun functionValueParameter(
        name: Name?,
        type: KaType,
    ): KaFunctionValueParameter = withValidityAssertion {
        KaBaseFunctionValueParameter(name, type)
    }

    override fun functionValueParameter(
        name: Name?,
        type: KaTypeCreator.() -> KaType,
    ): KaFunctionValueParameter = withValidityAssertion {
        KaBaseFunctionValueParameter(name, type())
    }

    override fun starTypeProjection(): KaStarTypeProjection = withValidityAssertion {
        KaBaseStarTypeProjection(token)
    }

    override fun arrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        with(analysisSession) {
            val builder = KaBaseArrayTypeBuilder.ByElementType(elementType, this@KaBaseTypeCreator).apply(init)

            val builderElementType = builder.elementType

            if (builderElementType is KaClassType && builder.shouldPreferPrimitiveTypes && !builderElementType.isMarkedNullable) {
                val classId = builderElementType.classId
                val primitiveArrayId =
                    StandardClassIds.primitiveArrayTypeByElementType[classId]
                        ?: StandardClassIds.unsignedArrayTypeByElementType[classId]
                if (primitiveArrayId != null) {
                    return typeCreator.classType(primitiveArrayId) {
                        isMarkedNullable = builder.isMarkedNullable
                        annotations(builder.annotations)
                    }
                }
            }

            return typeCreator.classType(StandardClassIds.Array) {
                isMarkedNullable = builder.isMarkedNullable
                typeArgument(builder.variance, builderElementType)
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
sealed class KaBaseTypeBuilder : KaTypeBuilder {
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
sealed class KaBaseClassTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaClassTypeBuilder, KaBaseTypeBuilder(),
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

    override fun invariantTypeArgument(type: () -> KaType) = withValidityAssertion {
        val type = type()
        backingTypeArguments += KaBaseTypeArgumentWithVariance(type, Variance.INVARIANT, type.token)
    }

    override fun invariantTypeArgument(type: KaType) = withValidityAssertion {
        backingTypeArguments += KaBaseTypeArgumentWithVariance(type, Variance.INVARIANT, type.token)
    }

    override fun typeArgument(variance: Variance, type: () -> KaType) = withValidityAssertion {
        val type = type()
        backingTypeArguments += KaBaseTypeArgumentWithVariance(type, variance, type.token)
    }

    override fun typeArgument(variance: Variance, type: KaType) = withValidityAssertion {
        backingTypeArguments += KaBaseTypeArgumentWithVariance(type, variance, type.token)
    }

    override fun typeArguments(arguments: () -> Iterable<KaTypeProjection>) = withValidityAssertion {
        backingTypeArguments += arguments()
    }

    override fun typeArgument(typeProjection: () -> KaTypeProjection) = withValidityAssertion {
        backingTypeArguments += typeProjection()
    }

    class ByClassId(classId: ClassId, typeCreatorDelegate: KaTypeCreator) : KaBaseClassTypeBuilder(typeCreatorDelegate) {
        val classId: ClassId by validityAsserted(classId)
    }

    class BySymbol(symbol: KaClassLikeSymbol, typeCreatorDelegate: KaTypeCreator) : KaBaseClassTypeBuilder(typeCreatorDelegate) {
        val symbol: KaClassLikeSymbol by validityAsserted(symbol)
    }
}

@KaImplementationDetail
sealed class KaBaseTypeParameterTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaTypeParameterTypeBuilder, KaBaseTypeBuilder(),
    KaTypeCreator by typeCreatorDelegate {
    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    class BySymbol(symbol: KaTypeParameterSymbol, typeCreatorDelegate: KaTypeCreator) :
        KaBaseTypeParameterTypeBuilder(typeCreatorDelegate) {
        val symbol: KaTypeParameterSymbol by validityAsserted(symbol)
    }
}

@KaImplementationDetail
sealed class KaBaseArrayTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaArrayTypeBuilder, KaBaseTypeBuilder(),
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

    class ByElementType(elementType: KaType, typeCreatorDelegate: KaTypeCreator) : KaBaseArrayTypeBuilder(typeCreatorDelegate) {
        val elementType: KaType by validityAsserted(elementType)
    }
}

@KaImplementationDetail
sealed class KaBaseCapturedTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaCapturedTypeBuilder, KaBaseTypeBuilder(),
    KaTypeCreator by typeCreatorDelegate {
    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    class Base(typeCreatorDelegate: KaTypeCreator) :
        KaBaseCapturedTypeBuilder(typeCreatorDelegate)
}

@KaImplementationDetail
sealed class KaBaseDefinitelyNotNullTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaDefinitelyNotNullTypeBuilder, KaBaseTypeBuilder(),
    KaTypeCreator by typeCreatorDelegate {

    class Base(typeCreatorDelegate: KaTypeCreator) : KaBaseDefinitelyNotNullTypeBuilder(typeCreatorDelegate)
}

@KaImplementationDetail
sealed class KaBaseFlexibleTypeBuilder(lowerBound: KaType, upperBound: KaType, typeCreatorDelegate: KaTypeCreator) : KaFlexibleTypeBuilder,
    KaBaseTypeBuilder(), KaTypeCreator by typeCreatorDelegate {
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

    class ByFlexibleType(type: KaFlexibleType, typeCreatorDelegate: KaTypeCreator) :
        KaBaseFlexibleTypeBuilder(type.lowerBound, type.upperBound, typeCreatorDelegate)

    class ByBounds(lowerBound: KaType, upperBound: KaType, typeCreatorDelegate: KaTypeCreator) :
        KaBaseFlexibleTypeBuilder(lowerBound, upperBound, typeCreatorDelegate)
}

@KaImplementationDetail
sealed class KaBaseIntersectionTypeBuilder(
    private val backingConjuncts: MutableSet<KaType> = mutableSetOf(),
    typeCreatorDelegate: KaTypeCreator
) :
    KaIntersectionTypeBuilder, KaBaseTypeBuilder(), KaTypeCreator by typeCreatorDelegate {

    private fun KaType.unwrapConjunct(): List<KaType> = (this as? KaIntersectionType)?.conjuncts ?: listOf(this)

    override val conjuncts: Set<KaType> get() = withValidityAssertion { backingConjuncts }

    override fun conjunct(conjunct: KaType): Unit = withValidityAssertion {
        backingConjuncts += conjunct.unwrapConjunct()
    }

    override fun conjunct(conjunct: () -> KaType) = withValidityAssertion {
        backingConjuncts += conjunct().unwrapConjunct()
    }

    override fun conjuncts(conjuncts: () -> Iterable<KaType>) = withValidityAssertion {
        backingConjuncts += conjuncts().flatMap { it.unwrapConjunct() }
    }

    override fun conjuncts(conjuncts: Iterable<KaType>) = withValidityAssertion {
        backingConjuncts += conjuncts.flatMap { it.unwrapConjunct() }
    }

    class ByIntersectionType(type: KaIntersectionType, typeCreatorDelegate: KaTypeCreator) :
        KaBaseIntersectionTypeBuilder(type.conjuncts.toMutableSet(), typeCreatorDelegate)

    class ByConjuncts(conjuncts: List<KaType>, typeCreatorDelegate: KaTypeCreator) :
        KaBaseIntersectionTypeBuilder(conjuncts.toMutableSet(), typeCreatorDelegate)
}

@KaImplementationDetail
class KaBaseDynamicTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaDynamicTypeBuilder,
    KaBaseTypeBuilder(), KaTypeCreator by typeCreatorDelegate

@KaImplementationDetail
sealed class KaBaseFunctionTypeBuilder(typeCreatorDelegate: KaTypeCreator, session: KaSession) : KaFunctionTypeBuilder, KaBaseTypeBuilder(),
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

    override fun contextParameter(contextParameter: () -> KaType) = withValidityAssertion {
        backingContextParameters += contextParameter()
    }

    private val backingValueParameters: MutableList<KaFunctionValueParameter> = mutableListOf()

    override val valueParameters: List<KaFunctionValueParameter>
        get() = withValidityAssertion { backingValueParameters }

    override fun valueParameter(parameter: KaFunctionValueParameter) = withValidityAssertion {
        backingValueParameters += parameter
    }

    override fun valueParameter(name: Name?, type: KaType) = withValidityAssertion {
        val valueParameter = KaBaseFunctionValueParameter(name, type)
        backingValueParameters += valueParameter
    }

    override fun valueParameter(name: Name?, type: () -> KaType) = withValidityAssertion {
        val valueParameter = KaBaseFunctionValueParameter(name, type())
        backingValueParameters += valueParameter
    }

    override fun valueParameter(parameter: () -> KaFunctionValueParameter) = withValidityAssertion {
        backingValueParameters += parameter()
    }

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

    class Base(typeCreatorDelegate: KaTypeCreator, session: KaSession) :
        KaBaseFunctionTypeBuilder(typeCreatorDelegate, session) {
    }
}
