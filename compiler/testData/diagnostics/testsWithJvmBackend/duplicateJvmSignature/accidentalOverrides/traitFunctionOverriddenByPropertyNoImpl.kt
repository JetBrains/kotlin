// COMPARE_WITH_LIGHT_TREE

interface T {
    fun getX(): Int
}

abstract class C : T {
    val x: Int
        <!ACCIDENTAL_OVERRIDE!>get()<!> = 1
}
