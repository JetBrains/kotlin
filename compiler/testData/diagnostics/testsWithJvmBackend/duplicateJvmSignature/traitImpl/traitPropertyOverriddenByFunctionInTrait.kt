// RUN_PIPELINE_TILL: KLIB
// FIR_IDENTICAL

interface T {
    val x: Int
        get() = 1
}

interface C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX() = 1<!>
}
