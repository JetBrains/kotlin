// WITH_STDLIB
// FILE: 1.kt

package test

public enum class X { A, B }

public inline fun switch(x: X): String = when (x) {
    X.A -> "O"
    X.B -> "K"
}

// FILE: 2.kt

import test.*

fun box(): String {
    return switch(X.A) + switch(X.B)
}
