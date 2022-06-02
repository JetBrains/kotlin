// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


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
        !<!UNINITIALIZED_VARIABLE!>x<!> // UNINITIALIZED_VARIABLE should be
        val a: Int = <!UNINITIALIZED_VARIABLE!>x1<!>.toInt() // UNINITIALIZED_VARIABLE should be
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
        <!VAL_REASSIGNMENT!>x<!> = (throw Exception()) || true //VAL_REASSIGNMENT should be
    } catch (e: Exception) {
    }
}
