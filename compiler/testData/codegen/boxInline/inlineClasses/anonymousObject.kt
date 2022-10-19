// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: 1.kt

package test

inline class IC(val value: Any)

inline fun <reified T> f(a: IC): () -> T = {
    a.value as T
}

// FILE: 2.kt

import test.*

fun box(): String = f<String>(IC("OK"))()
