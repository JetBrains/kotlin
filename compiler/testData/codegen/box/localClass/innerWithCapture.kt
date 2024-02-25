/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-66105: SyntaxError: Identifier 'box' has already been declared
// IGNORE_BACKEND: WASM
// WITH_STDLIB

import kotlin.test.*

fun box(s: String): String {
    class Local {
        open inner class Inner() {
            open fun result() = s
        }
    }

    return Local().Inner().result()
}

fun box(): String {
    return box("OK")
}