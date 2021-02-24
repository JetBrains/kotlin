// !LANGUAGE: +NewInference +ProhibitInvisibleAbstractMethodsInSuperclasses
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

// TESTCASE NUMBER: 1
class Case1() : BaseCase1(), InterfaceCase1 {}

abstract class BaseCase1() {
    abstract fun foo(): String
    abstract val a: String
}

interface InterfaceCase1 {
    fun foo() = "foo"
    val a: String
        get() = "a"
}

// TESTCASE NUMBER: 2
class Case2Outer {
    val v = "v"

    abstract class Case2Base() {
        abstract fun foo(): String
    }

    inner
    class A() : Case2Base() {

    }
}

// TESTCASE NUMBER: 3
fun case3() {
    object : CaseOuter.CaseBase() {}.outerFoo()
}

class B() : CaseOuter.CaseBase() {}

sealed class CaseOuter {
    val v = "v"
    abstract fun outerFoo();

    abstract class CaseBase() : CaseOuter() {
        abstract fun foo(): String
    }

    class A() : CaseBase() {
    }
}
