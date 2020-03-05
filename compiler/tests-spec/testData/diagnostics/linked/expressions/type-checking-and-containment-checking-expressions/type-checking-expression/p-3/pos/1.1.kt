// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -USELESS_IS_CHECK USELESS_NULLABLE_CHECK -UNUSED_VALUE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_VARIABLE -USELESS_NULLABLE_CHECK
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, type-checking-and-containment-checking-expressions, type-checking-expression -> paragraph 3 -> sentence 1
 * RELEVANT PLACES: expressions, type-checking-and-containment-checking-expressions, type-checking-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Type-checking expression always has type kotlin.Boolean.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1() {
    val x = null is Nothing
    x checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 2
fun case2() {
    ("" !is String) checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 3
fun case3(n: Nothing) {
    val x = n is Nothing
    x checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 4
fun case4() {
    val x = A() is Any?
    x checkType { check<Boolean>() }
}

class A