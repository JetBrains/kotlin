// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FILE: first.kt

package first

enum class First {
    ONE, TWO;
}

val THREE = First.ONE

// FILE: main.kt

package second

import first.First
import first.THREE

enum class Second {
    THREE, FOUR;
}

val ONE = Second.THREE

fun foo(f: First) = when (f) {
    ONE -> 1
    TWO -> 2
}

fun bar(s: Second) = when (s) {
    THREE -> 3
    FOUR -> 4
}
