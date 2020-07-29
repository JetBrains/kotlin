// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -WRONG_MODIFIER_TARGET
// SKIP_TXT


// TESTCASE NUMBER: 1
open class B1(val x: Int)
abstract class A1 : B1(1)

fun case1() {
    val a = A1()
}

// TESTCASE NUMBER: 2
open class B2(val x: Int)

fun case2() {
    abstract class A1 : B2(1)

    val a = A1()
}


// TESTCASE NUMBER: 3
fun case3() {
    open class B(val x: Int)

    inner abstract class A : B(1)

    val a = A()
}


// TESTCASE NUMBER: 4
class Case4() {
    abstract class A1 : B1(1)
    open class B1(val x: Int)

    fun test() {
        val a = A1()
    }
}
