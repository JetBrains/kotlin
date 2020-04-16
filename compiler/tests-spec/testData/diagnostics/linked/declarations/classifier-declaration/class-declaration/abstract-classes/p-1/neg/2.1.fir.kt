// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 1 -> sentence 2
 * RELEVANT PLACES: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 1 -> sentence 1
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
    val b0 = Base0()
    val b1 = Base1()
    val b2 = Base2(1, "1")
}
