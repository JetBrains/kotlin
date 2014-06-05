open class B {
    fun getX() = 1
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE!>val x: Int<!> = 1
}