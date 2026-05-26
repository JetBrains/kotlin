// MODULE: lib
// FILE: first.kt
// HEADER_MODE
package lib

class AnotherClass

class Dependency {
    fun hello(): String {
        return "Hello"
    }
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Dependency
import lib.AnotherClass

class App {
    fun run() {
        val another = AnotherClass()
        val dep = Dependency()
        val res = dep.hello()
    }
}
