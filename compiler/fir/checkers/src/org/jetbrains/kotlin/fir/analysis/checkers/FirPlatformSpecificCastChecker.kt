/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirPlatformSpecificCastChecker : FirSessionComponent {
    abstract fun shouldSuppressImpossibleCast(
        session: FirSession,
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        generalApplicabilityChecker: TypeOperationApplicabilityChecker,
    ): Boolean

    abstract fun shouldSuppressImpossibleIsCheck(
        session: FirSession,
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        generalApplicabilityChecker: TypeOperationApplicabilityChecker,
    ): Boolean

    object Default : FirPlatformSpecificCastChecker() {
        override fun shouldSuppressImpossibleCast(
            session: FirSession,
            fromType: ConeKotlinType,
            toType: ConeKotlinType,
            generalApplicabilityChecker: TypeOperationApplicabilityChecker
        ): Boolean {
            return false
        }

        override fun shouldSuppressImpossibleIsCheck(
            session: FirSession,
            fromType: ConeKotlinType,
            toType: ConeKotlinType,
            generalApplicabilityChecker: TypeOperationApplicabilityChecker
        ): Boolean {
            return false
        }
    }
}

val FirSession.firPlatformSpecificCastChecker: FirPlatformSpecificCastChecker
        by FirSession.sessionComponentAccessorWithDefault(FirPlatformSpecificCastChecker.Default)
