interface T {
    fun getX() = 1
}

interface C : T {
    <!ACCIDENTAL_OVERRIDE!>val x: Int
        get() = 1<!>
}
