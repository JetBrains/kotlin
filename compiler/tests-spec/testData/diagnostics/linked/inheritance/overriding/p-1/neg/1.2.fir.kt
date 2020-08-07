// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
open class BaseCase1()

val BaseCase1.foo: Int
    get() = 1

val BaseCase1.boo: Int
    get() = 1

open class ChildCase1 : BaseCase1() {
    override val foo: Int = TODO()
    override val boo: Int = TODO()
}

fun case1(b: BaseCase1, c: ChildCase1) {
    b.foo
    b.boo
    c.foo
    c.boo
}

// TESTCASE NUMBER: 2
open class BaseCase2()

var BaseCase2.foo: Int
    get() = 1
    set(value) {}

var BaseCase2.boo: Int
    get() = 1
    set(value) {}

open class ChildCase2 : BaseCase2() {
    override var foo: Int = TODO()
}

fun case2(b: BaseCase2, c: ChildCase2) {
    b.foo
    c.foo
}
