// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNREACHABLE_CODE

class A {
    class B {
        class C
    }
}

fun test(a: A.<!SYNTAX!><!>): A.<!SYNTAX!><!> {
    val aa: A. <!SYNTAX!>=<!><!SYNTAX!><!> null!!
}

fun test1(a: A.B.<!SYNTAX!><!>): A.B.<!SYNTAX!><!> {
    val aa: A.B. <!SYNTAX!>=<!><!SYNTAX!><!> null!!
}

fun test2(a: A.<!UNRESOLVED_REFERENCE!>e<!>.C): A.<!UNRESOLVED_REFERENCE!>e<!>.C {
    val aa: A.<!UNRESOLVED_REFERENCE!>e<!>.C = null!!
}

fun test3(a: <!UNRESOLVED_REFERENCE!>a<!>.A.C): <!UNRESOLVED_REFERENCE!>a<!>.A.C {
    val aa: <!UNRESOLVED_REFERENCE!>a<!>.A.C = null!!
}

fun test4(a: A.B.<!UNRESOLVED_REFERENCE!>ee<!>): A.B.<!UNRESOLVED_REFERENCE!>ee<!> {
    val aa: A.B.<!UNRESOLVED_REFERENCE!>ee<!> = null!!
}

fun test5(a: A.<!UNRESOLVED_REFERENCE!>ee<!>): A.<!UNRESOLVED_REFERENCE!>ee<!> {
    val aa: A.<!UNRESOLVED_REFERENCE!>ee<!> = null!!
}
