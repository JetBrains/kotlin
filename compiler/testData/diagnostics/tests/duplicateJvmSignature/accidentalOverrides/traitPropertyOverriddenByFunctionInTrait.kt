trait T {
    val x: Int
        get() = 1
}

trait C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}