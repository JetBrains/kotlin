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

fun test2(a: <!OTHER_ERROR!>A.e.C<!>): <!OTHER_ERROR!>A.e.C<!> {
    val aa: <!OTHER_ERROR!>A.e.C<!> = null!!
}

fun test3(a: <!OTHER_ERROR!>a.A.C<!>): <!OTHER_ERROR!>a.A.C<!> {
    val aa: <!OTHER_ERROR!>a.A.C<!> = null!!
}

fun test4(a: <!OTHER_ERROR!>A.B.ee<!>): <!OTHER_ERROR!>A.B.ee<!> {
    val aa: <!OTHER_ERROR!>A.B.ee<!> = null!!
}

fun test5(a: <!OTHER_ERROR!>A.ee<!>): <!OTHER_ERROR!>A.ee<!> {
    val aa: <!OTHER_ERROR!>A.ee<!> = null!!
}