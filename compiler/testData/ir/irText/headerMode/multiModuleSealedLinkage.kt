// MODULE: lib
// FILE: first.kt
// HEADER_MODE
package lib

private sealed interface SealedInterface
class Implementation : SealedInterface

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Implementation

class App {
    fun run() {
        val impl = Implementation()
    }
}
