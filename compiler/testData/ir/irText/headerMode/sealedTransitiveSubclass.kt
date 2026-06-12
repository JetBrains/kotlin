// MODULE: lib
// HEADER_MODE
// FILE: first.kt
package lib

private sealed interface GrandParent {
    sealed interface Parent : GrandParent {
        class Leaf : Parent
    }
}

fun myFunc() {
    val impl = GrandParent.Parent.Leaf()
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
