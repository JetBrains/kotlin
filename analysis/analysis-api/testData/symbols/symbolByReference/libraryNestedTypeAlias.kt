// IGNORE_FE10
// LANGUAGE: +NestedTypeAliases
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt

class C(val p: String)

class Foo {
    typealias TA = C
}

// MODULE: main(lib)
// FILE: usage.kt

import Foo.T<caret>A

fun main(): String {
    val c: TA = TA("OK")
    return c.p
}