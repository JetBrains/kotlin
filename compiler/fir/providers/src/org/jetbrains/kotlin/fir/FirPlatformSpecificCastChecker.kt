/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirPlatformSpecificCastChecker : FirSessionComponent {
    abstract fun shouldSuppressImpossibleCast(session: FirSession, fromType: ConeKotlinType, toType: ConeKotlinType): Boolean

    object Default : FirPlatformSpecificCastChecker() {
        override fun shouldSuppressImpossibleCast(session: FirSession, fromType: ConeKotlinType, toType: ConeKotlinType): Boolean {
            return false
        }
    }
}

val FirSession.firPlatformSpecificCastChecker: FirPlatformSpecificCastChecker by FirSession.sessionComponentAccessor()
