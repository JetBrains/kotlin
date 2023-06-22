interface B {
    fun getX() = 1
}

interface D {
    <!ACCIDENTAL_OVERRIDE!>val x: Int<!>
}

class C(d: D) : D by d, B
