// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, equality-expressions, value-equality-expressions -> paragraph 3 -> sentence 1
 * RELEVANT PLACES: expressions, equality-expressions, value-equality-expressions -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Value equality expressions always have type kotlin.Boolean as does the equals method in kotlin.Any
 * HELPERS: checkType
 */
data class A(val a: Boolean)


// TESTCASE NUMBER: 1
fun case1() {
    val x = A(false) == A(true)
    x checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 2
fun case2() {
    val x = A(false) == A(false)
    x checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 3
fun case3() {
    val x = true == false
    x checkType { check<Boolean>() }
}