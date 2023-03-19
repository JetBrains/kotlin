// MODULE: lib
// FILE: A.kt

package aaa

enum class E {
    TRIVIAL_ENTRY,
    SUBCLASS { }
}

// MODULE: main(lib)
// FILE: B.kt

import aaa.E

fun box(): String {
    if (E.TRIVIAL_ENTRY == E.SUBCLASS) return "Fail"
    return "OK"
}
