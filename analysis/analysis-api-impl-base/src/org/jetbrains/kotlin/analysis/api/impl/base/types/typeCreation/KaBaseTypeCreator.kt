/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaArrayTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
abstract class KaBaseTypeCreator<T : KaSession> : KaBaseSessionComponent<T>(), KaTypeCreator {
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
                    return buildClassType(primitiveArrayId) {
                        isMarkedNullable = builder.isMarkedNullable
                    }
                }
            }

            return buildClassType(StandardClassIds.Array) {
                isMarkedNullable = builder.isMarkedNullable
                argument(builderElementType, builder.variance)
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