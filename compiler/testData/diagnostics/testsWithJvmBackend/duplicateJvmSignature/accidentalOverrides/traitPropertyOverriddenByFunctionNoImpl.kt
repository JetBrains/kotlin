// RUN_PIPELINE_TILL: KLIB
// FIR_IDENTICAL

interface T {
    val x: Int
}

abstract class C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX() = 1<!>
}
