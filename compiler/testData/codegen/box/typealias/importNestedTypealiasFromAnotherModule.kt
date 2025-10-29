// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-79519
// LANGUAGE: +NestedTypeAliases
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0

// MODULE: lib
// FILE: lib.kt

class C(val p: String)

class Foo {
    typealias TA = C
}

// MODULE: main(lib)
// FILE: main.kt

import Foo.TA

fun box(): String {
    val c: TA = TA("OK")
    return c.p
}
