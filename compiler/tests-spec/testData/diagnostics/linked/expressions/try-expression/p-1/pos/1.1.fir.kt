// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK:expressions, try-expression -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: expressions, try-expression -> paragraph 1 -> sentence 2
 * expressions, try-expression -> paragraph 1 -> sentence 5
 * NUMBER: 1
 * DESCRIPTION: try-expression has to start with a try body
 */
fun throwException(): Nothing = throw Exception()


// TESTCASE NUMBER: 1

fun case1() {
    try {
        throwException()
    }finally {
        "a"
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    try {
        val a = throwException()
    }catch (e: Exception) {
        "a"
    }
}
