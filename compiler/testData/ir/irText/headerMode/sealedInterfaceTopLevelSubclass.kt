// MODULE: lib
// HEADER_MODE
// FILE: first.kt
package lib

private sealed interface SealedInterface

class TopLevelImplementation : SealedInterface

fun myFunc() {
    val impl = TopLevelImplementation()
}

// MODULE: main(lib)
// FILE: main.kt
package main
import lib.myFunc

class App {
    fun run() {
        myFunc()
    }
}
