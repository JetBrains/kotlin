// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, elvis-operator-expression -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The type of elvis operator expression is the least upper bound of the non-nullable variant of the type of the left-hand side expression and the type of the right-hand side expression.
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1
fun case1() {
    val x = null ?: getNull()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
}

fun getNull(): Boolean? = null


// TESTCASE NUMBER: 2
fun case2() {
    val x = A(mutableSetOf({ false }, { println("") })).b ?: false
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
}

class A(val b: Set<Any>? = null)

// TESTCASE NUMBER: 3
fun case3() {
    val x = null?: throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
}

// TESTCASE NUMBER: 4
fun case4() {
    val x = null <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
}
