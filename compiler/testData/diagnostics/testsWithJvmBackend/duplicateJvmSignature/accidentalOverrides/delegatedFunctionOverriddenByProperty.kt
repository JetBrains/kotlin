interface B {
    fun getX() = 1
}

interface D {
    val x: Int
}

class <!ACCIDENTAL_OVERRIDE!>C(d: D)<!> : D by d, B