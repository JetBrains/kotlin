/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

abstract class FirIdentityLessPlatformDeterminer : FirSessionComponent {
    abstract fun isIdentityLess(typeInfo: TypeInfo): Boolean

    object Default : FirIdentityLessPlatformDeterminer() {
        override fun isIdentityLess(typeInfo: TypeInfo): Boolean = typeInfo.isPrimitive
    }
}

val FirSession.identityLessPlatformDeterminer: FirIdentityLessPlatformDeterminer by FirSession.sessionComponentAccessor()
