// COMPARE_WITH_LIGHT_TREE

open class B {
    val x: Int
        get() = 1
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>fun getX()<!> = 1<!>
}
