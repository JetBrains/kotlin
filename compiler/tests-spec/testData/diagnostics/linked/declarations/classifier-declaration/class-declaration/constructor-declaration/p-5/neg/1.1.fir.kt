// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1<T>() {
    class A(t: <!UNRESOLVED_REFERENCE!>T<!>)
    class B(x: List<<!UNRESOLVED_REFERENCE!>T<!>>)
    class C(c: () -> <!UNRESOLVED_REFERENCE!>T<!>)
    class E(n: Nothing, t: <!UNRESOLVED_REFERENCE!>T<!>)
}

// TESTCASE NUMBER: 2
class Case2<T>() {
    data class A(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: <!UNRESOLVED_REFERENCE!>T<!><!>)
    data class B(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>x: List<<!UNRESOLVED_REFERENCE!>T<!>><!>)
    data class C(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>c: () -> <!UNRESOLVED_REFERENCE!>T<!><!>)
    data class E(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>n: Nothing<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: <!UNRESOLVED_REFERENCE!>T<!><!>)
}

// TESTCASE NUMBER: 3
class Case3<T>() {
    enum class A(t: <!UNRESOLVED_REFERENCE!>T<!>)
    enum class B(x: List<<!UNRESOLVED_REFERENCE!>T<!>>)
    enum class C(c: () -> <!UNRESOLVED_REFERENCE!>T<!>)
    enum class E(n: Nothing, t: <!UNRESOLVED_REFERENCE!>T<!>)
}
