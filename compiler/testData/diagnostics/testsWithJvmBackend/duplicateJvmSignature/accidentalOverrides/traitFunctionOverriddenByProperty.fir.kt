interface T {
    fun getX() = 1
}

class C : T {
    <!ACCIDENTAL_OVERRIDE!>val x: Int
        get() = 1<!>
}
