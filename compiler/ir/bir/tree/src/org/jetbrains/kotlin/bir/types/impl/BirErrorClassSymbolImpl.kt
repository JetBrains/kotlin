/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.impl

import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.ir.util.IdSignature

object BirErrorClassSymbolImpl : BirClassSymbol {
    override val signature: IdSignature?
        get() = null
}
