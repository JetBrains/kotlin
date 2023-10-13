data class A(<!CONFLICTING_OVERLOADS!>val x: Int<!>, <!CONFLICTING_OVERLOADS!>val y: String<!>) {
    <!CONFLICTING_OVERLOADS!>fun component1()<!> = 1
    <!CONFLICTING_OVERLOADS!>fun component2()<!> = 2
}