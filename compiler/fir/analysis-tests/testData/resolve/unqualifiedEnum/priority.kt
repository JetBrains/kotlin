// LANGUAGE: +ContextSensitiveEnumResolutionInWhen
// FILE: first.kt

package first

enum class First {
    ONE, TWO;
}

val THREE = First.ONE

// FILE: second.kt

package second

import first.First
import first.THREE

enum class Second {
    THREE, FOUR;
}

val ONE = Second.THREE

fun foo(f: First) = <!NO_ELSE_IN_WHEN!>when<!> (f) {
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>ONE<!> -> 1
    TWO -> 2
}

fun bar(s: Second) = <!NO_ELSE_IN_WHEN!>when<!> (s) {
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>THREE<!> -> 3
    FOUR -> 4
}

