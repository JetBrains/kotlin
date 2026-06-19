// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: foo.kt
package test

enum class Foo { BAR }

// FILE: main.kt
package main

import test.Foo
import test.Foo.*

fun expectsFoo(foo: Foo) {}

fun usage(): Foo {
    expectsFoo(BAR)
    return BAR
}
