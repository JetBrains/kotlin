/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.FqName

public abstract class KtPackageSymbol : KtSymbol {
    public abstract val fqName: FqName

    abstract override fun createPointer(): KtSymbolPointer<KtPackageSymbol>
}