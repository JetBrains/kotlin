// MODULE: main
// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: rex

// FILE: generated.kt
// RESOLVE_EXTENSION_FILE
package rex

// RESOLVE_EXTENSION_CLASSIFIER: RexClass
class RexClass {
    fun member(): Int = 0
}

// RESOLVE_EXTENSION_CALLABLE: rexCallable
fun rexCallable(): Int = 0

// FILE: main.kt
package main

import rex.*

class MainClass : Any() {
    fun foo(): Int = RexClass().member() + rexCallable()
}
