package c

fun test1() {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>r<!>: Nothing =<!> null!!
}

fun test2(a: A) {
    a + a
    <!UNREACHABLE_CODE!>bar()<!>
}

fun test3() {
    null!!
    <!UNREACHABLE_CODE!>bar()<!>
}

fun throwNPE() = null!!

class A {
    fun plus(<!UNUSED_PARAMETER!>a<!>: A): Nothing = throw Exception()
}

fun bar() {}