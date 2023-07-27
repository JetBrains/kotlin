interface T {
    fun getX(): Int
}

abstract class C : T {
    <!ACCIDENTAL_OVERRIDE!>val x: Int
        get() = 1<!>
}
