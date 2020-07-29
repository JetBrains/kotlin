// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes-declarations -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: abstract classes cannot be instantiated directly
 */
// TESTCASE NUMBER: 1
abstract class A1

fun case1() {
    val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
}

// TESTCASE NUMBER: 2
fun case2() {
    abstract class A1

    val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
}


// TESTCASE NUMBER: 3
class case3(val x: Int) {
    abstract inner class A1
    val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>

    companion object{
        val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
    }
}


// TESTCASE NUMBER: 4
class Case4() {
    abstract class A1

    fun test() {
        val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
    }

    companion object{
        val a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>A1()<!>
    }
}

