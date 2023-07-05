/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
package lib

inline val lib: String
    get() = "lib"

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib.length
}