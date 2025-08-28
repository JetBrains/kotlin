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
                    }
                }
            }

            return typeCreator.classType(StandardClassIds.Array) {
                isMarkedNullable = builder.isMarkedNullable
                typeArgument(builder.variance, builderElementType)
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
sealed class KaBaseClassTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaClassTypeBuilder, KaTypeCreator by typeCreatorDelegate {
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
sealed class KaBaseTypeParameterTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaTypeParameterTypeBuilder,
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
sealed class KaBaseArrayTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaArrayTypeBuilder,
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
sealed class KaBaseCapturedTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaCapturedTypeBuilder, KaTypeCreator by typeCreatorDelegate {
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
sealed class KaBaseDefinitelyNotNullTypeBuilder(typeCreatorDelegate: KaTypeCreator) : KaDefinitelyNotNullTypeBuilder,
    KaTypeCreator by typeCreatorDelegate {

    class Base(typeCreatorDelegate: KaTypeCreator) : KaBaseDefinitelyNotNullTypeBuilder(typeCreatorDelegate)
}

@KaImplementationDetail
sealed class KaBaseFlexibleTypeBuilder(lowerBound: KaType, upperBound: KaType, typeCreatorDelegate: KaTypeCreator) : KaFlexibleTypeBuilder,
    KaTypeCreator by typeCreatorDelegate {
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
    KaIntersectionTypeBuilder, KaTypeCreator by typeCreatorDelegate {

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
