/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.NameTable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

fun <T> wasmNameTable() = NameTable<T>(sanitizer = ::sanitizeWatIdentifier)

fun sanitizeWatIdentifier(ident: String): String {
    if (ident.isEmpty())
        return "_"
    if (ident.all(::isValidWatIdentifier))
        return ident
    return ident.map { if (isValidWatIdentifier(it)) it else "_" }.joinToString("")
}

// https://webassembly.github.io/spec/core/text/values.html#text-id
fun isValidWatIdentifier(c: Char): Boolean =
    c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z'
            // TODO: SpiderMonkey js shell can't parse some of the
            //  permitted identifiers: '?', '<'
            // || c in "!#$%&′*+-./:<=>?@\\^_`|~"
            || c in "$.@_"

