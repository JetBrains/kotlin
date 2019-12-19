// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: declarations, property-declaration, local-property-declaration -> paragraph 1 -> sentence 1
 * RELEVANT PLACES:  declarations, property-declaration, property-initialization -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: All non-abstract properties must be definitely initialized before their first use.
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35565
 */
fun case1() {
    val x1: String
    val x: Boolean
    try {
        val x0: Boolean = (throw Exception()) || true
        !x // UNINITIALIZED_VARIABLE should be
        val a: Int = x1.toInt() // UNINITIALIZED_VARIABLE should be
    } catch (e: Exception) {
    }
}
/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35565
 */
fun case2() {
    val x: Boolean = false
    try {
        x = (throw Exception()) || true //VAL_REASSIGNMENT should be
    } catch (e: Exception) {
    }
}