/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.FqName

abstract class KtPackageSymbol : KtSymbol {
    abstract val fqName: FqName

    abstract override fun createPointer(): KtSymbolPointer<KtPackageSymbol>
}