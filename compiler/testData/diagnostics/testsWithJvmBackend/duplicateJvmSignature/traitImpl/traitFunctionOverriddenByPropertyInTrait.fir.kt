// COMPARE_WITH_LIGHT_TREE

interface T {
    fun getX() = 1
}

interface C : T {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>val x: Int<!>
        get() = 1<!>
}
