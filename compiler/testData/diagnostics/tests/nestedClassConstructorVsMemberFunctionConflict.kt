// FIR_IDENTICAL
// ISSUE: KT-62005

class A {
    class NestedInA<!CONFLICTING_OVERLOADS!>()<!>
    <!CONFLICTING_OVERLOADS!>fun NestedInA()<!> {}

    class <!CONFLICTING_OVERLOADS!>NestedInA2<!>
    <!CONFLICTING_OVERLOADS!>fun NestedInA2()<!> {}
}
