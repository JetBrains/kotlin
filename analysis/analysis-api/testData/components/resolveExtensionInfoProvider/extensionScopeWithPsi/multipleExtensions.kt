// MODULE: rex1
// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: rex1

// FILE: rex1.kt
// RESOLVE_EXTENSION_FILE
package rex1

// RESOLVE_EXTENSION_CLASSIFIER: RexClass1
interface RexClass1

// RESOLVE_EXTENSION_CALLABLE: rexCallable1
fun rexCallable1(): Any = TODO()

// MODULE: rex2
// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: rex2

// FILE: rex2.kt
// RESOLVE_EXTENSION_FILE
package rex2

// RESOLVE_EXTENSION_CLASSIFIER: RexClass2
interface RexClass2

// RESOLVE_EXTENSION_CALLABLE: rexCallable2
fun rexCallable2(): Any = TODO()

// MODULE: main(rex1, rex2)
// FILE: main.kt
package main

import rex1.*
import rex2.*

object MainObject : RexClass1, RexClass2 {
    fun foo(): List<Any> = listOf(rexCallable1(), rexCallable2())
}