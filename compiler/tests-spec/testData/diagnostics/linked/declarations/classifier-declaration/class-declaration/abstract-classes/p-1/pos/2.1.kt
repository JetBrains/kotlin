// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check abstract classes can have abstract not implemented members
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

// TESTCASE NUMBER: 2
abstract class Case2() : BaseCase2() {
    abstract fun boo()
    public abstract override fun foo()
}

abstract class BaseCase2() {
    abstract val a: String
    protected abstract fun foo()
}

// TESTCASE NUMBER: 3

interface MyInterfaceCase3 {
    abstract fun foo(): String
    abstract val a: String
}

abstract class MyImplCase3() : MyInterfaceCase3 {
    abstract fun boo()
}

