// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

// TESTCASE NUMBER: 1

fun case1(): String {
    var flag = false
    try {
        flag = true
    } catch (e: Exception) {
       return "foo"
    } finally {
        return "FINALLY"
    }
    return "return"
}
