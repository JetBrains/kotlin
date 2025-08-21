// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-79519
// LANGUAGE: +NestedTypeAliases

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
