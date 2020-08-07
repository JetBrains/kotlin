// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
open class BaseCase1(val a: Int, val b: CharSequence) {
    open fun foo(): Int = TODO()
}

open class ChildCase1a : BaseCase1(1, "") {
    override fun foo() = 2.0
}

open class ChildCase1b : BaseCase1(1, "") {
    override fun foo(): Any = TODO()
}

// TESTCASE NUMBER: 2
open class BaseCase2(val a: Int, val b: CharSequence) {
    open fun foo(): Double = TODO()
}

open class ChildCase2 : BaseCase2(1, "") {
    override open fun foo(): Int = TODO()
}

// TESTCASE NUMBER: 3
open class BaseCase3(val a: Int, val b: CharSequence) {
    open fun foo(): Int = TODO()
}

open class ChildCase3 : BaseCase3(1, "") {
    override fun foo(): String = TODO()
}
