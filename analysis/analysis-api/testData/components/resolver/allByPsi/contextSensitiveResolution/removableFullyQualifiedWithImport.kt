// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: foo.kt
package test

enum class Foo { BAR }

fun expectsFoo(foo: Foo) {}

// FILE: main.kt
package main

import test.expectsFoo
import test.Foo.BAR

fun usage() {
    expectsFoo(test.Foo.BAR)
}
