data class A(val x: Int, val y: String) {
    <!CONFLICTING_OVERLOADS!>fun component1()<!> = 1
    <!CONFLICTING_OVERLOADS!>fun component2()<!> = 2
}
