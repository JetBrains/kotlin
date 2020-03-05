open class B {
    val x: Int
        get() = 1
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}