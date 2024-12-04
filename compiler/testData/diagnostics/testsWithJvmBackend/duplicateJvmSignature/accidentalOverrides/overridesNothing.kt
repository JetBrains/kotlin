// IGNORE_FIR_DIAGNOSTICS
// IGNORE_ERRORS

interface B {
    fun getX() = 1
}

class C : B {
    <!NOTHING_TO_OVERRIDE!>override<!> <!ACCIDENTAL_OVERRIDE!>val x = 1<!>
}
