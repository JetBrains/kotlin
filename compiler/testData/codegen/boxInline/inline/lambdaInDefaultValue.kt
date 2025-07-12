/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt

inline fun inlineFun(param: String, lambda: (String) -> String = { it }): String {
    return lambda(param)
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    return inlineFun("OK")
}
