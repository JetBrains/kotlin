/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession

abstract class PlatformSupertypeUpdater : FirSessionComponent {
    abstract fun updateSupertypesIfNeeded(firClass: FirClass, scopeSession: ScopeSession)
}

val FirSession.platformSupertypeUpdater: PlatformSupertypeUpdater? by FirSession.nullableSessionComponentAccessor()
