/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.FqName

/**
 * [KaPackageSymbol] represents a package as a symbol.
 */
public abstract class KaPackageSymbol : KaSymbol {
    /**
     * The package's fully qualified name.
     */
    public abstract val fqName: FqName

    final override val location: KaSymbolLocation
        get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }

    abstract override fun createPointer(): KaSymbolPointer<KaPackageSymbol>
}
