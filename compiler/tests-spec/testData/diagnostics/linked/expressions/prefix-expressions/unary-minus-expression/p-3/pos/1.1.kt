// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, prefix-expressions, unary-minus-expression -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: No additional restrictions apply for unary minus
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1
class Case1(var a: Int) {
    operator fun unaryMinus(): Nothing? { TODO() }
}

fun case1() {
    val a = -Case1(1)
}

// TESTCASE NUMBER: 2
class Case2(var a: Int) {
    operator fun unaryMinus(): Any? { TODO() }
}

fun case2() {
    val a = -Case2(1)
}

