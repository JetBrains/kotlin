/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

abstract class FirTypeScope : FirScope() {
    // Initially, this method is intended to belong only to this class, but not to FirScope
    // But the main-use case (FirSyntheticPropertiesScope) uses it on arbitrary type (intersection and others) that currently use
    // scope implementations for type-unrelated scopes as well
    // One of the idea how to fix it is considered extracting it to the interface
    //
    // The idea behind this class and abstract override is to explicitly state that those implementations should implement this method properly
    // and use other FirOverrideAwareScope when delegating to them (as in FirClassSubstitutionScope)
    abstract override fun processOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>) -> ProcessorAction
    ): ProcessorAction
}
