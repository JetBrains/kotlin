/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: base.kt
package base

inline fun base() {}

// FILE: lib.kt
package lib

import base.*

inline fun lib() {
    base()
}

// FILE: main.kt
package test

import lib.*

fun foo() {
    lib()
}