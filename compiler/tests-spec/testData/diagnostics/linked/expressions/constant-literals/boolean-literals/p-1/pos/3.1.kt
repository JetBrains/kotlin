/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, boolean-literals -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Checking of type for Boolean values
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    true checkType { check<Boolean>() }
    false checkType { check<Boolean>() }

    checkSubtype<Boolean>(true)
    checkSubtype<Boolean>(false)
}
