package com.example

class Foo {
    companion object {
        operator fun invoke(param: Int) = Foo() // constructor
    }
}

fun main() {
    Foo(1)
    com.example.Foo(2)

    Foo.Companion(3)
    com.example.Foo.Companion(4)
}
