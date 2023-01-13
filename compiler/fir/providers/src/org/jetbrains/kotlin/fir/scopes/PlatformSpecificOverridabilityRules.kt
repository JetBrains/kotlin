/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction

interface PlatformSpecificOverridabilityRules : FirSessionComponent {
    // Thus functions return "null" in case the status should be defined via standard platform-independent rules
    fun isOverriddenFunction(
        overrideCandidate: FirSimpleFunction,
        baseDeclaration: FirSimpleFunction
    ): Boolean?

    fun isOverriddenProperty(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirProperty
    ): Boolean?
}

val FirSession.platformSpecificOverridabilityRules: PlatformSpecificOverridabilityRules? by FirSession.nullableSessionComponentAccessor()
