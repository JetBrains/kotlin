/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.name.Name

abstract class FirSyntheticNamesProvider : FirSessionComponent {
    abstract fun possibleGetterNamesByPropertyName(name: Name): List<Name>
    abstract fun setterNameByGetterName(name: Name): Name
    abstract fun possiblePropertyNamesByAccessorName(name: Name): List<Name>
}

val FirSession.syntheticNamesProvider: FirSyntheticNamesProvider by FirSession.sessionComponentAccessor()
