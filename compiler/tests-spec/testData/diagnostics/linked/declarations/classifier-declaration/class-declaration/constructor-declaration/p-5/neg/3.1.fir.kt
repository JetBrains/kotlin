// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1<T>() {
    class A(var t: <!UNRESOLVED_REFERENCE!>T<!>)
    class B(var x: List<<!UNRESOLVED_REFERENCE!>T<!>>)
    class C(var c: () -> <!UNRESOLVED_REFERENCE!>T<!>)
    class E(var n: Nothing, var t: <!UNRESOLVED_REFERENCE!>T<!>)
}
