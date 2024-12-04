// FILE: lib.kt
package lib

class Foo {
    fun foo() {}
}

// FILE: main.kt
package main

import lib.Foo

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        <caret>Foo().foo()
    }
}