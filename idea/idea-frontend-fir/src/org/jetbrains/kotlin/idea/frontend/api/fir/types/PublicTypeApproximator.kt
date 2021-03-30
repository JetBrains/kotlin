/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

internal object PublicTypeApproximator {
    fun approximateTypeToPublicDenotable(
        type: ConeKotlinType,
        session: FirSession,
    ): ConeKotlinType? {
        val approximator = session.inferenceComponents.approximator
        return approximator.approximateToSuperType(type, PublicApproximatorConfiguration) as ConeKotlinType?
    }

    private object PublicApproximatorConfiguration : TypeApproximatorConfiguration.AllFlexibleSameValue() {
        override val allFlexible: Boolean get() = false
        override val errorType: Boolean get() = true
        override val definitelyNotNullType: Boolean get() = false
        override val integerLiteralType: Boolean get() = true
        override val intersectionTypesInContravariantPositions: Boolean get() = true
        override val localTypes: Boolean get() = true
    }
}