// MODULE: lib
// HEADER_MODE
// FILE: first.kt
package lib

private sealed interface SealedInterface {
    class Implementation : SealedInterface
}

fun myFunc() {
    val impl = SealedInterface.Implementation()
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
