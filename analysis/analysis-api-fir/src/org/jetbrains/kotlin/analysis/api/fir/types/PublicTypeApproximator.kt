/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

internal object PublicTypeApproximator {
    fun approximateTypeToPublicDenotable(
        type: ConeKotlinType,
        session: FirSession,
        approximateLocalTypes: Boolean
    ): ConeKotlinType? {
        val approximator = session.typeApproximator
        return approximator.approximateToSuperType(type, PublicApproximatorConfiguration(approximateLocalTypes))
    }

    internal class PublicApproximatorConfiguration(
        override val approximateLocalTypes: Boolean
    ) : TypeApproximatorConfiguration.AllFlexibleSameValue() {
        override val approximateAllFlexible: Boolean get() = true
        override val approximateErrorTypes: Boolean get() = false
        override val approximateDefinitelyNotNullTypes: Boolean get() = true
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
        override val approximateAnonymous: Boolean get() = true
    }
}
