// COMPARE_WITH_LIGHT_TREE

interface T {
    fun getX() = 1
}

interface C : T {
    val x: Int
        <!ACCIDENTAL_OVERRIDE!>get()<!> = 1
}
