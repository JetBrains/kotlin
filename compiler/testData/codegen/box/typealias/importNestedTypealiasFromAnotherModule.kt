// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
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

import Foo.<!UNRESOLVED_IMPORT!>TA<!>

fun box(): String {
    val c: <!UNRESOLVED_REFERENCE!>TA<!> = <!UNRESOLVED_REFERENCE!>TA<!>("OK")
    return c.<!UNRESOLVED_REFERENCE!>p<!>
}
