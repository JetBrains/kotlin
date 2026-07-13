// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: base.kt
package test

sealed class Base {
    class Child : Base()
}

// FILE: main.kt
package main

import test.Base
import test.Base.*

fun usage(b: Base) {
    if (b is Child) {
        "".hashCode()
    }
}
