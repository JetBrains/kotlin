open class B {
    fun getX() = 1
}

class C(<!ACCIDENTAL_OVERRIDE!>val x: Int<!>) : B()