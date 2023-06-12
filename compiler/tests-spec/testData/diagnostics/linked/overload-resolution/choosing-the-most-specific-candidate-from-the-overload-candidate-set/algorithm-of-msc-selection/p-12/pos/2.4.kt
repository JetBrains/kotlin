// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 12 -> sentence 2
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 3
 * built-in-types-and-their-semantics, built-in-integer-types-1, integer-type-widening -> paragraph 3 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: call with implicit receiver (built-in integer types extesnsion): different built-in integer types and one of them is kotlin.Int
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1
private fun Int.boo(x: Int): String = TODO() //(1.1)
private fun Int.boo(x: Long): Unit = TODO() //(1.2)
private fun Int.boo(x: Short): Unit = TODO() //(1.3)
private fun Int.boo(x: Byte): Unit = TODO() //(1.4)

fun case1() {
    1.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: testPackCase1.boo; typeCall: extension function")!>boo(1)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(1)<!>
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: testPackCase1.boo; typeCall: extension function")!>boo(x = 1)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(x = 1)<!>
    }
}


// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackCase2

class Case2 {
    private fun Int.boo(x: Int, y: Int): String = TODO() //(1.1)
    private fun Short.boo(x: Int, y: Int): Unit = TODO() //(1.2)
    private fun Long.boo(x: Int, y: Int): Unit = TODO() //(1.3)
    private fun Byte.boo(x: Int, y: Int): Unit = TODO() //(1.4)

    fun case2() {
        1.apply {
            //to (1.1)
            <!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.boo; typeCall: extension function")!>boo(1, 2)<!>
            //(1.1) return type is String
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(1, 2)<!>
            //to (1.1)
            <!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.boo; typeCall: extension function")!>boo(x = 1, y = 2)<!>
            //(1.1) return type is String
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(x = 1, y = 2)<!>
        }
    }
}




// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackCase4

private operator fun Int.minus(x: String): String = TODO() //(1.1)
private operator fun Short.minus(x: String): Unit = TODO() //(1.2)
private operator fun Byte.minus(x: String): Unit = TODO() //(1.3)
private operator fun Long.minus(x: String): Unit = TODO() //(1.4)

fun case4() {
    1.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: testPackCase4.minus; typeCall: operator extension function")!>minus("1")<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>minus("1")<!>
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: testPackCase4.minus; typeCall: operator extension function")!>this-"1"<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>this-"1"<!>
    }
}
