// DONT_STOP_ON_FIR_ERRORS
interface B {
    fun getX() = 1
}

class C : B {
    <!NOTHING_TO_OVERRIDE!>override<!> <!ACCIDENTAL_OVERRIDE!>val x = 1<!>
}
