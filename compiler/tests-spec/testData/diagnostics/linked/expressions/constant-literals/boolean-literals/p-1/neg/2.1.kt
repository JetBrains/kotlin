// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, constant-literals, boolean-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: checking of type for Boolean values: impossible to use as identifiers
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1

fun case1() {
    val <!SYNTAX!>false<!> : Any = false
    val <!SYNTAX!>true<!> : Any = true
}

// TESTCASE NUMBER: 2

fun case2() {
    var <!SYNTAX!>false<!> : Any = false
    var <!SYNTAX!>true<!> : Any = true
}

// TESTCASE NUMBER: 3

fun case3() {
    fun <!SYNTAX!>false<!>() {}
    fun <!SYNTAX!>true<!>() {}
}

// TESTCASE NUMBER: 4

fun case4() {
    fun <!SYNTAX!>false<!>() = "false"
    fun <!SYNTAX!>true<!>() = "true"
}

// TESTCASE NUMBER: 5

fun case5() {
    class <!SYNTAX!>false<!> (){}
    class <!SYNTAX!>true<!> (){}
}

// TESTCASE NUMBER: 6

fun case6() {
    class <!SYNTAX!>false<!> : <!SYNTAX!>true<!>{}
}

interface <!SYNTAX!>true<!>{}
interface <!SYNTAX!>false<!>{}

// TESTCASE NUMBER: 7

fun case7() {
    class <!SYNTAX!>false<!> : <!SYNTAX!>false<!>{}
}