// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_VARIABLE -UNUSED_PARAMETER -FINAL_UPPER_BOUND
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 8 -> sentence 1
 * RELEVANT PLACES: expressions, try-expression -> paragraph 9 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The type of the try-expression is the least upper bound of the types of the last expressions of the try body and the last expressions of all the catch blocks
 */
fun throwExceptionA(b: Boolean) = run { if (b) throw ExcA() }

open class A<T>(var data: T) {
    fun foo(d: A<T>) {}
}

class B<T>(data: T) : A<T>(data)

// TESTCASE NUMBER: 1

fun case1() {
    val tryVal: B<String> =
    <!TYPE_MISMATCH, TYPE_MISMATCH!>try {
        throwExceptionA(false)
        A("")
    } catch (e: Exception) {
        B("")
    }<!>


}

// TESTCASE NUMBER: 2

fun case2() {
    val tryVal: A<String> =
    <!TYPE_MISMATCH, TYPE_MISMATCH!>try {
        throwExceptionA(false)
        A("")
    } catch (e: Exception) {
        null
    }<!>
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35494
 */
fun case3() {
    val tryVal: A<Int> =
    <!TYPE_MISMATCH, TYPE_MISMATCH!>try {
        throwExceptionA(false)
        A(2)
    } catch (e: ExcA) {
        A(<!NULL_FOR_NONNULL_TYPE, NULL_FOR_NONNULL_TYPE!>null<!>) //diag duplication
    } catch (e: ExcB) {
        B(<!NULL_FOR_NONNULL_TYPE, NULL_FOR_NONNULL_TYPE!>null<!>) //diag duplication
    }<!>
}

class ExcA() : Exception()
class ExcB() : Exception()
