// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: built-in-types-and-their-semantics, built-in-integer-types-1, integer-type-widening -> paragraph 3 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: prefer kotlin.Short to kotlin.Byte.
 */

// TESTCASE NUMBER: 1
class Case1() {
    fun foo(x: Short): String = TODO() // (1.1)
    fun foo(x: Byte): Unit = TODO() // (1.2)
    fun case1() {
        <!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo(1)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(1)<!>
    }
}

fun case1(case: Case1) {
    case.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo(1)<!>
    case.foo(1)
}

// TESTCASE NUMBER: 2
class Case2() {
    fun foo(vararg x: Short): String = TODO() // (1.1)
    fun foo(vararg x: Byte): Unit = TODO() // (1.2)
    fun case2(case: Case2) {
        <!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(1, 1)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(1, 1)<!>
    }
}

fun case2(case: Case2) {
    case.<!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(1, 1)<!>
    case.foo(1, 1)
}

// TESTCASE NUMBER: 3
class Case3() {
    fun foo(vararg x: Short): String = TODO() // (1.2)
    fun foo(x: Byte): Unit = TODO() // (1.1)
    fun case3(case: Case3) {
        <!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo(1)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(1)<!>
    }
}

fun case3(case: Case3) {
    case.<!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo(1)<!>
    case.foo(1)
}


// TESTCASE NUMBER: 4
class Case4() {
    infix fun foo(x: Short): String = TODO() // (1.1)
    infix fun foo(x: Byte): Unit = TODO() // (1.2)
    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>this foo 1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>this foo 1<!>
        <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>foo(1)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(1)<!>
        this.<!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>foo(1)<!>
        this.foo(1)
    }
}

fun case4(case: Case4) {
    <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>case foo 1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case foo 1<!>
    case.<!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>foo( 1)<!>
    case.foo( 1)
}
