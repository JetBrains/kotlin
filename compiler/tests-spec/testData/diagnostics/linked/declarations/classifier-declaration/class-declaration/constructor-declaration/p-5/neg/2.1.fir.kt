// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1<T>() {
    class A(val t: <!UNRESOLVED_REFERENCE!>T<!>)
    class B(val x: List<<!UNRESOLVED_REFERENCE!>T<!>>)
    class C(val c: () -> <!UNRESOLVED_REFERENCE!>T<!>)
    class E(val n: Nothing, val t: <!UNRESOLVED_REFERENCE!>T<!>)
}
