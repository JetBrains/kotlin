// RUN_PIPELINE_TILL: KLIB
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

open class B {
    fun foo(l: List<String>) {}
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE!>fun foo(l: List<Int>) {}<!>
}
