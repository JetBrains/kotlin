// RUN_PIPELINE_TILL: KLIB
interface T {
    fun getX() = 1
}

class C : T {
    <!ACCIDENTAL_OVERRIDE!>val x = 1<!>
}
