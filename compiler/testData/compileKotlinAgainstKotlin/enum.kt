// FILE: A.kt

package aaa

enum class E {
    TRIVIAL_ENTRY,
    SUBCLASS { }
}

// FILE: B.kt

import aaa.E

fun main(args: Array<String>) {
    if (E.TRIVIAL_ENTRY == E.SUBCLASS) throw AssertionError()
}
