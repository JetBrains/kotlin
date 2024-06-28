// FIR_IDENTICAL
interface B {
    fun getX() = 1
}

interface D {
    val x: Int
}

<!ACCIDENTAL_OVERRIDE!>class C(d: D) : D by d, B<!>
