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

fun box(): String {
    if (TRIVIAL_ENTRY == SUBCLASS) return "Fail 1"
    if (Nested().fortyTwo() != 42) return "Fail 2"
    return "OK"
}
