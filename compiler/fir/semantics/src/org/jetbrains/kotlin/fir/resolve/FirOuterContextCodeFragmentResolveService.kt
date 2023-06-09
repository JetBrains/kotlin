/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirLocalScopes

interface FirOuterContextCodeFragmentResolveService : FirSessionComponent {
    val localScopes: FirLocalScopes
    val nonLocalTowerDataElements: List<org.jetbrains.kotlin.fir.declarations.FirTowerDataElement>
}

val FirSession.outerContextCodeFragmentResolveService: FirOuterContextCodeFragmentResolveService by FirSession.sessionComponentAccessor()