// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1

fun case1() {
    var a = Case1()
    val res: Any? = ++a
}


class Case1() {

    operator fun inc(): B {
        TODO()
    }
}

class B() {}

// TESTCASE NUMBER: 2

fun case2() {
    var a = Case2()
    val res: Any? = ++a
}

class Case2() : C() {
    var i = 0

    operator fun inc(): C {
        TODO()
    }

}

open class C() {}
