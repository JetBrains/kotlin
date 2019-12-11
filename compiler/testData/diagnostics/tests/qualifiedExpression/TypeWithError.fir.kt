// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNREACHABLE_CODE
// !WITH_NEW_INFERENCE

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

fun test2(a: A.e.C): A.e.C {
    val aa: A.e.C = null!!
}

fun test3(a: a.A.C): a.A.C {
    val aa: a.A.C = null!!
}

fun test4(a: A.B.ee): A.B.ee {
    val aa: A.B.ee = null!!
}

fun test5(a: A.ee): A.ee {
    val aa: A.ee = null!!
}