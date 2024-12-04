// MODULE: lib
// FILE: lib.kt
package lib

interface Key {
    val s get() = "OK"
}

internal class Internal {
    companion object : Key
}

// MODULE: box(lib)
// FILE: box.kt
package app

import lib.*

fun foo(key: Key) = key.s

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun box() = foo(Internal)