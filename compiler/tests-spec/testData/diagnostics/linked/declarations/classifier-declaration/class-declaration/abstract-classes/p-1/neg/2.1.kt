// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check abstract classes cannot be instantiated directly
 */

// TESTCASE NUMBER: 1
abstract class Base0()

abstract class Base1() {
    abstract fun foo()
}

abstract class Base2(var b1: Any, val a1: Any) {
    abstract fun foo()
}

fun case1() {
    val b0 = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Base0()<!>
    val b1 = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Base1()<!>
    val b2 = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Base2(1, "1")<!>
}

