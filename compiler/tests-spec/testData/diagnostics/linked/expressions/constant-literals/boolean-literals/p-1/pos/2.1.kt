// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, constant-literals, boolean-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: checking of type for Boolean values: possible to use as identifiers if surround with backticks
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1

fun case1() {
    val `false` : Any = false
    val `true` : Any = true
}

// TESTCASE NUMBER: 2

fun case2() {
    var `false` : Any = false
    var `true` : Any = true
}

// TESTCASE NUMBER: 3

fun case3() {
    fun `false`() {}
    fun `true`() {}
}

// TESTCASE NUMBER: 4

fun case4() {
    fun `false`() = "false"
    fun `true`() = "true"
}

// TESTCASE NUMBER: 5

fun case5() {
    class `false` (){}
    class `true` (){}
}


// TESTCASE NUMBER: 6

fun case6() {
    class `false` : `true`{}
}

interface `true`{}
interface `false`{}


// TESTCASE NUMBER: 7

fun case7() {
    class `true` : `false`{}
}