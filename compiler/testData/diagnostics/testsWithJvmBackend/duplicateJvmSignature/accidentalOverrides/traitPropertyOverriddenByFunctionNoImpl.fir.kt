// COMPARE_WITH_LIGHT_TREE

interface T {
    val x: Int
}

abstract class C : T {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>fun getX()<!> = 1<!>
}
