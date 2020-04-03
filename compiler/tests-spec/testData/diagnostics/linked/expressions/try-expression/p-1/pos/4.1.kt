// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 1 -> sentence 4
 * RELEVANT PLACES: expressions, try-expression -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: try-expression has to start with a try body, catch blocks and finally block
 */
fun throwException(): Nothing = throw Exception()
class ExcA() : Exception()
class ExcB() : Exception()

// TESTCASE NUMBER: 1

fun case1() {
    try {
        throwException()
    } catch (e: ExcA) {
    } catch (e: ExcB) {
    } finally {
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    try {
        throwException()
    } finally {
    }
}