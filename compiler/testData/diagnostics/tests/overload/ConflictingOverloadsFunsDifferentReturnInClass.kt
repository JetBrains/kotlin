class A {
    <!CONFLICTING_OVERLOADS!>fun a(<!UNUSED_PARAMETER!>a<!>: Int): Int<!> = 0

    <!CONFLICTING_OVERLOADS!>fun a(<!UNUSED_PARAMETER!>a<!>: Int)<!> {
    }
}
