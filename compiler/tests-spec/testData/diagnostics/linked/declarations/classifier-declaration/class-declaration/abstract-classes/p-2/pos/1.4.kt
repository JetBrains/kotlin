// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 * HELPERS: checkType, functions
 */

// TESTCASE NUMBER: 1

fun case1() {
    val c = Case1()
    c.a
    c.foo()
}

abstract class BaseCase1() {
    fun foo() = "foo"
    val a: String
        get() = "a"
}

interface MyInterface1 {
    abstract fun foo(): String
    abstract val a: String
}

class Case1() : BaseCase1(), MyInterface1 {}

// TESTCASE NUMBER: 2

fun case2() {
    Case2Outer().A().foo()
}

class Case2Outer {
    val v = "v"

    abstract class Case2Base() {
        abstract fun foo(): String
    }

    inner class A() : Case2Base() {
        override fun foo(): String {
            return v
        }
    }
}

// TESTCASE NUMBER: 3

fun case3() {
    CaseOuter.A().outerFoo()
    B().outerFoo()
    object : CaseOuter.CaseBase() {
        override fun foo(): String {
            return "c"
        }

        override fun outerFoo() {}

    }.outerFoo()
}

sealed class CaseOuter {
    val v = "v"
    abstract fun outerFoo();

    abstract class CaseBase() : CaseOuter() {
        abstract fun foo(): String
    }

    class A() : CaseBase() {
        override fun foo(): String {
            return "A"
        }

        override fun outerFoo() {
            println("outerFoo")
        }
    }
}

class B() : CaseOuter.CaseBase() {
    override fun foo(): String {
        return ""
    }

    override fun outerFoo() {
    }
}
