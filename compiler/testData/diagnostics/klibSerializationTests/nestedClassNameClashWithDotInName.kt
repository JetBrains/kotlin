// ISSUE: KT-85639
// TARGET_BACKEND: JS_IR
// A dot in the name of a nested class makes its signature clash with the signature of another nested class:
// both A.`B.C` and A.B.C get the signature `lib/A.B.C|null[0]`, since class signatures have no hash.
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib
// FILE: lib.kt
package lib

class A {
    <!CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR!>class `B.C` {
        <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun value(): String = "dotted"<!>
    }<!>

    class B {
        <!CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR!>class C {
            <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun value(): String = "nested"<!>
        }<!>
    }
}

// MODULE: main(lib)
// FILE: main.kt
import lib.A

fun box(): String {
    if (A.`B.C`().value() != "dotted") return "FAIL: A.`B.C`"
    if (A.B.<!UNRESOLVED_REFERENCE!>C<!>().value() != "nested") return "FAIL: A.B.C"
    return "OK"
}
