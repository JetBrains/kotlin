// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 3 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: enum entry can have their own body
 */

// TESTCASE NUMBER: 1
enum class Case1 {
    VAL1() {
        override fun foo1() {}
    },
    VAL2() {
        fun foo2() {}
    };

    open fun foo1() {}
}

// TESTCASE NUMBER: 2

enum class Case2 {
    VAL1() {
        var foo1 = 1  //(1)

        fun f() {
            foo1 // to (1)
            foo1.invoke()// to (1).(3)
            foo1() // to (3)
        }
    },
    VAL2() {
        inner class A(){}
        fun foo2() {} //(2)

        fun f() {
            foo2 // to (4)
            foo2.invoke() //to (4).(5)
            foo2() // to (2)
        }
    };

    open fun foo1() {} //(3)
    var foo2 = 1 // (4)
}

operator fun Int.invoke() {} //(5)

fun case2() {
    Case2.VAL1.<!DEBUG_INFO_CALL("fqName: Case2.foo1; typeCall: function")!>foo1()<!>
    Case2.VAL2.<!DEBUG_INFO_CALL("fqName: invoke; typeCall: variable&invoke")!>foo2()<!>
}
