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
 * NUMBER: 6
 * DESCRIPTION: infix call (built-in integer types extesnsion): different built-in integer types and one of them is kotlin.Int
 */

// TESTCASE NUMBER: 1
class Case1{
    infix fun Int.boo(x: Int): String = TODO() //(1.1)
    infix fun Long.boo(x: Long): Unit = TODO() //(1.2)
    infix fun Short.boo(x: Short): Unit = TODO() //(1.3)
    infix fun Byte.boo(x: Byte): Unit = TODO() //(1.4)

    fun case(){
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix extension function")!>1 boo 1<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>1 boo 1<!>

        //to (1.1)
        1.<!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix extension function")!>boo(1)<!>
        //(1.1) return type is String
        1.boo(1)
    }
}

fun case1(case: Case1) {
    case.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix extension function")!>1 boo 1<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>1 boo 1<!>

        1.apply{
            //to (1.1)
            <!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix extension function")!>this boo 1<!>
            //(1.1) return type is String
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>this boo 1<!>
        }
    }



}
