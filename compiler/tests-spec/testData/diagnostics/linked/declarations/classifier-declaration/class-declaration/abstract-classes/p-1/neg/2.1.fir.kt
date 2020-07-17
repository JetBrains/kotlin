// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

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
