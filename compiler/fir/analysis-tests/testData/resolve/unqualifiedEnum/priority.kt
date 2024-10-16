// LANGUAGE: +ExpectedTypeGuidedResolution
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

fun foo(f: First) = when (f) {
    _.ONE -> 1
    _.TWO -> 2
}

fun bar(s: Second) = when (s) {
    _.THREE -> 3
    _.FOUR -> 4
}

