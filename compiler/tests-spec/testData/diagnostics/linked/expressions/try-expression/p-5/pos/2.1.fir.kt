// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-23680
 */
fun case1(): Int {
    var a = 1
    try {
        throw Exception() //invalid UNREACHABLE_CODE diagnostic
    } catch (e: Exception) {
        a = 5
        return++a
    } finally {
        return a
    }
    return 0
}
