// COMPARE_WITH_LIGHT_TREE

interface T {
    fun getX(): Int
}

abstract class C : T {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>val x: Int<!>
        get() = 1<!>
}
