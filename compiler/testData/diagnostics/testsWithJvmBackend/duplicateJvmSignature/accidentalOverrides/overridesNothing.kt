// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: KLIB

interface B {
    fun getX() = 1
}

class C : B {
    <!NOTHING_TO_OVERRIDE!>override<!> <!ACCIDENTAL_OVERRIDE!>val x = 1<!>
}
