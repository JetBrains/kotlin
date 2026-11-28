// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// A dot in the name of a member function gives it the same declaration FqName as a function of a nested class:
// both A.`B.c`() and A.B.c() are `lib/A.B.c`. Unlike class signatures, function signatures also contain a hash
// of the mangled name, which includes the dotted simple name, so the signatures don't clash.

// MODULE: lib
// FILE: lib.kt
package lib

class A {
    fun `B.c`(): String = "dotted"

    class B {
        fun c(): String = "nested"
    }
}

// MODULE: main(lib)
// FILE: main.kt
import lib.A

fun box(): String {
    if (A().`B.c`() != "dotted") return "FAIL: A.`B.c`"
    if (A.B().c() != "nested") return "FAIL: A.B.c"
    return "OK"
}
