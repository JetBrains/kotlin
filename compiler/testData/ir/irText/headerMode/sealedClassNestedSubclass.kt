// MODULE: lib
// HEADER_MODE
// FILE: first.kt
package lib

private sealed class SealedClass {
    class Implementation : SealedClass()
}

fun myFunc() {
    val impl = SealedClass.Implementation()
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
