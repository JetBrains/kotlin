// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes-declarations -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, abstract-classes-declarations -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION:  abstract members are implemented in a subobject of the abstract class
 */

// TESTCASE NUMBER: 1
abstract class A1 {
    abstract val v: Int
    abstract var c1: List<Int>
    abstract fun foo()
    abstract fun <T>foo1() : T
}

object C1 : A1(){
    override val v: Int
        get() = TODO()
    override var c1: List<Int>
        get() = TODO()
        set(value) {}

    override fun foo() {
        TODO()
    }

    override fun <T> foo1(): T {
        TODO()
    }
}