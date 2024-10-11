// IGNORE_FIR_DIAGNOSTICS

interface B {
    fun getX() = 1
}

class C : B {
    <!NOTHING_TO_OVERRIDE!>override<!> <!ACCIDENTAL_OVERRIDE!>val x = 1<!>
}
