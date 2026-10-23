/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaArrayTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.fir.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
sealed class KaBaseClassTypeBuilder : KaClassTypeBuilder {
    private val backingArguments = mutableListOf<KaTypeProjection>()

    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    override val arguments: List<KaTypeProjection> get() = withValidityAssertion { backingArguments }

    override fun argument(argument: KaTypeProjection): Unit = withValidityAssertion {
        backingArguments += argument
    }

    override fun argument(type: KaType, variance: Variance): Unit = withValidityAssertion {
        backingArguments += KaBaseTypeArgumentWithVariance(type, variance, type.token)
    }

    @KaImplementationDetail
    class ByClassId(classId: ClassId, override val token: KaLifetimeToken) : KaBaseClassTypeBuilder() {
        private val backingClassId: ClassId = classId

        val classId: ClassId get() = withValidityAssertion { backingClassId }
    }

    @KaImplementationDetail
    class BySymbol(symbol: KaClassLikeSymbol, override val token: KaLifetimeToken) : KaBaseClassTypeBuilder() {
        private val backingSymbol: KaClassLikeSymbol = symbol

        val symbol: KaClassLikeSymbol get() = withValidityAssertion { backingSymbol }
    }
}

internal sealed class KaBaseArrayTypeBuilder : KaArrayTypeBuilder {
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

    class ByElementType(elementType: KaType, override val token: KaLifetimeToken) : KaBaseArrayTypeBuilder() {
        private val backingElementType: KaType = elementType

        val elementType: KaType get() = withValidityAssertion { backingElementType }
    }
}

@KaImplementationDetail
sealed class KaBaseTypeParameterTypeBuilder : KaTypeParameterTypeBuilder {
    override var isMarkedNullable: Boolean = false
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion {
                field = value
            }
        }

    @KaImplementationDetail
    class BySymbol(symbol: KaTypeParameterSymbol, override val token: KaLifetimeToken) : KaBaseTypeParameterTypeBuilder() {
        private val backingSymbol: KaTypeParameterSymbol = symbol

        val symbol: KaTypeParameterSymbol get() = withValidityAssertion { backingSymbol }
    }
}
