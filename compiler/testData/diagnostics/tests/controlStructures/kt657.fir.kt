//KT-657 Semantic checks for when without condition
package kt657

class Pair<A, B>(a: A, b: B)

fun foo() =
    when {
        cond1() -> 12
        cond2() -> 2
        4 -> 34
        Pair(1, 2) -> 3
        in 1..10 -> 34
        4 -> 38
        is Int -> 33
        else -> 34
    }

fun cond1() = false

fun cond2() = true
