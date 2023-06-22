// COMPARE_WITH_LIGHT_TREE

open class B {
    fun getX() = 1
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>val x: Int<!>
        get() = 1<!>
}
