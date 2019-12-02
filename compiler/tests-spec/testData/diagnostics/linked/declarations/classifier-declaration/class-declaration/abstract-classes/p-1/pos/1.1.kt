// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check abstract classes can have abstract not implemented members
 * ISSUES: KT-27825
 */

// TESTCASE NUMBER: 1

abstract class Base() {
    abstract fun foo()

    abstract val a: String
}

class Impl : Base() {
    override fun foo(): Unit {
        TODO("not implemented")
    }

    override val a: String
        get() = TODO("not implemented")

}

fun case1() {
    val impl = Impl()
}