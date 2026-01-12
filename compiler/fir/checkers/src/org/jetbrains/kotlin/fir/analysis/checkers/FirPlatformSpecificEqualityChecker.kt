/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirPlatformSpecificEqualityChecker : FirSessionComponent {
    abstract fun shouldSuppressInapplicableEquality(
        leftType: ConeKotlinType,
        rightType: ConeKotlinType,
        generalApplicabilityChecker: TypeOperationApplicabilityChecker,
    ): Boolean

    object Default : FirPlatformSpecificEqualityChecker() {
        override fun shouldSuppressInapplicableEquality(
            leftType: ConeKotlinType,
            rightType: ConeKotlinType,
            generalApplicabilityChecker: TypeOperationApplicabilityChecker
        ): Boolean {
            return false
        }
    }
}

val FirSession.firPlatformSpecificEqualityChecker: FirPlatformSpecificEqualityChecker by FirSession.sessionComponentAccessor()
