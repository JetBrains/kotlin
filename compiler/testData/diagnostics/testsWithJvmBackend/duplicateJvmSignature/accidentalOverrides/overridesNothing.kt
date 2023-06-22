// COMPARE_WITH_LIGHT_TREE

interface B {
    fun getX() = 1
}

class C : B {
    <!ACCIDENTAL_OVERRIDE!><!NOTHING_TO_OVERRIDE!>override<!> val x<!> = 1
}
