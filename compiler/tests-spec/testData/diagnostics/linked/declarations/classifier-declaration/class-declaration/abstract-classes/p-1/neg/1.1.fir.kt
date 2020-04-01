// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check abstract classes can have abstract not implemented members
 */

// TESTCASE NUMBER: 1

abstract class Base() {
    abstract fun foo() = {}
    fun boo() : Unit
    abstract val a = ""
    val b
    var d
}

class Impl : Base() {
    override fun foo(): () -> Unit {
        TODO("not implemented")
    }

    override val a: String
        get() = TODO("not implemented")

}

fun case1() {
    val impl = Impl()
}
