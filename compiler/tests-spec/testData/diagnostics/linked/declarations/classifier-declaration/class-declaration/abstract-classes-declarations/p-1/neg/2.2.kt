// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -WRONG_MODIFIER_TARGET
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes-declarations -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: abstract classes cannot be instantiated directly
 */

// TESTCASE NUMBER: 1
open class B1(val x: Int)
abstract class A1 : B1(1)

fun case1() {
    val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
}

// TESTCASE NUMBER: 2
open class B2(val x: Int)

fun case2() {
    abstract class A1 : B2(1)

    val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
}


// TESTCASE NUMBER: 3
fun case3() {
    open class B(val x: Int)

    inner abstract class A : B(1)

    val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A()<!>
}


// TESTCASE NUMBER: 4
class Case4() {
    abstract class A1 : B1(1)
    open class B1(val x: Int)

    fun test() {
        val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
    }
}



