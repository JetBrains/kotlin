/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

sealed class FirTypeSpecificityComparatorProvider : FirComposableSessionComponent<FirTypeSpecificityComparatorProvider> {
    abstract val typeSpecificityComparator: TypeSpecificityComparator

    class Simple(override val typeSpecificityComparator: TypeSpecificityComparator) : FirTypeSpecificityComparatorProvider(), FirComposableSessionComponent.Single<FirTypeSpecificityComparatorProvider> {}

    class Composed(
        override val components: List<FirTypeSpecificityComparatorProvider>,
    ) : FirTypeSpecificityComparatorProvider(), FirComposableSessionComponent.Composed<FirTypeSpecificityComparatorProvider> {
        override val typeSpecificityComparator: TypeSpecificityComparator =
            TypeSpecificityComparator.Composed(components.map { it.typeSpecificityComparator })
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirTypeSpecificityComparatorProvider>): Composed {
        return Composed(components)
    }

    companion object {
        fun of(typeSpecificityComparator: TypeSpecificityComparator): FirTypeSpecificityComparatorProvider {
            return Simple(typeSpecificityComparator)
        }
    }
}

val FirSession.typeSpecificityComparatorProvider: FirTypeSpecificityComparatorProvider? by FirSession.nullableSessionComponentAccessor()
