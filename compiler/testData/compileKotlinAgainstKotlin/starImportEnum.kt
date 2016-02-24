// FILE: A.kt

package aaa

enum class E {
    TRIVIAL_ENTRY,
    SUBCLASS { };

    class Nested {
        fun fortyTwo() = 42
    }
}

// FILE: B.kt

import aaa.E.*

fun main(args: Array<String>) {
    if (TRIVIAL_ENTRY == SUBCLASS) throw AssertionError()
    if (Nested().fortyTwo() != 42) throw AssertionError()
}
