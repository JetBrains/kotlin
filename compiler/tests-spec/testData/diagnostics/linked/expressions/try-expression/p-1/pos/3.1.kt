// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 1 -> sentence 3
 * RELEVANT PLACES: expressions, try-expression -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: try-expression has to start with a try body and continue with zero ore more catch blocks
 */
fun throwException(): Nothing = throw Exception()

// TESTCASE NUMBER: 1

fun case1() {
    try {
        val a = ""
    } catch (e: Exception) {
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    try {
        val a = ""
        throwException()
    } catch (e: IllegalArgumentException) {
    } catch (e: ExcA) {
    } catch (e: ExcB) {
    }
}

class ExcA() : Exception()
class ExcB() : Exception()