trait T {
    fun getX() = 1
}

trait C : T {
    val x: Int
        <!ACCIDENTAL_OVERRIDE!>get()<!> = 1
}