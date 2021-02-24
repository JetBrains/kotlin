// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

class A() {
    var i = 0

    operator fun dec(): A {
        this.i--
        return this
    }
}

// TESTCASE NUMBER: 1

fun case1() {
    var b: Case1? = Case1()
    <!UNSAFE_CALL!>--<!>b?.a
}


class Case1() {
    var a: A = A()
}

// TESTCASE NUMBER: 2

fun case2() {
    var b= Case2()
    --b.a
}

class Case2() {
    val a = A()
}
