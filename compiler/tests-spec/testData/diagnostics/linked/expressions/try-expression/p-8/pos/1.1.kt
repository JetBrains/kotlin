// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 8 -> sentence 1
 * RELEVANT PLACES: expressions, try-expression -> paragraph 9 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The type of the try-expression is the least upper bound of the types of the last expressions of the try body and the last expressions of all the catch blocks
 * HELPERS: checkType
 */

fun throwExceptionA(b: Boolean) = run { if (b) throw ExcA() }

open class A<T>(var data: T) {}

class B<T>(data: T) : A<T>(data)

// TESTCASE NUMBER: 1

fun case1() {
    val tryVal =
        try {
            throwExceptionA(false)
            A("")
        } catch (e: Exception) {
            B("")
        }

    <!DEBUG_INFO_EXPRESSION_TYPE("A<kotlin.String>")!>tryVal<!>
    tryVal checkType { check<A<kotlin.String>>() }
}

// TESTCASE NUMBER: 2

fun case2() {
    val tryVal =
        try {
            throwExceptionA(false)
            A("")
        } catch (e: Exception) {
            null
        }
    <!DEBUG_INFO_EXPRESSION_TYPE("A<kotlin.String>?")!>tryVal<!>
    tryVal checkType { check<A<kotlin.String>?>() }

}

// TESTCASE NUMBER: 3

fun case3() {
    val tryVal =
        try {
            throwExceptionA(false)
            A(2)
        } catch (e: ExcA) {
            A(0)
        } catch (e: ExcB) {
            B(null)
        }

    <!DEBUG_INFO_EXPRESSION_TYPE("A<out kotlin.Int?>")!>tryVal<!>
    tryVal checkType { check<A<out kotlin.Int?>>() }
}

class ExcA() : Exception()
class ExcB() : Exception()