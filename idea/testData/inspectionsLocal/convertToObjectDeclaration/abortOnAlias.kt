// PROBLEM: none

import A as T

class <caret>A(val i: Int) {
    companion object {
        const val B = 1
    }
}

fun test() {
    val t = T(42)
}