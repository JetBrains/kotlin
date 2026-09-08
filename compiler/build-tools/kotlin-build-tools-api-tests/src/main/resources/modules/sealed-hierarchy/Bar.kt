package org.example

sealed class Bar : Foo("bar") {
    abstract fun run()
}

class Baz : Bar() {
    override fun run() {
        println("Foo")
    }
}
