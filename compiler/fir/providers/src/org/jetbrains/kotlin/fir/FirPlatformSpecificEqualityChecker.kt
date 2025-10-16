/*
 * Copyright 2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirPlatformSpecificEqualityChecker : FirSessionComponent {
    abstract fun shouldSuppressInapplicableEquality(leftType: ConeKotlinType, rightType: ConeKotlinType): Boolean

    object Default : FirPlatformSpecificEqualityChecker() {
        override fun shouldSuppressInapplicableEquality(leftType: ConeKotlinType, rightType: ConeKotlinType): Boolean {
            return false
        }
    }
}

val FirSession.firPlatformSpecificEqualityChecker: FirPlatformSpecificEqualityChecker by FirSession.sessionComponentAccessor()
