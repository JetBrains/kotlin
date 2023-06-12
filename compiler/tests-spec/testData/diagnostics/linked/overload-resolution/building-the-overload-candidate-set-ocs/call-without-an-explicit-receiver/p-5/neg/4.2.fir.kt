// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 4
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 6
 * NUMBER: 2
 * DESCRIPTION: Top-level non-extension functions named f: callables implicitly imported into the current file;
 */


// FILE: TestCase1.kt
// TESTCASE NUMBER: 3

package tests.case1

import lib.case1.*

interface I
//class A : I
class B : I

fun case1(){
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!><!OPERATOR_MODIFIER_REQUIRED!>A<!>()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case1.A.invoke; typeCall: function")!><!OPERATOR_MODIFIER_REQUIRED!>A<!>()<!>
}

// FILE: Lib.kt
package lib.case1

//fun A() : String = ""

object A {
    /*operator*/ fun invoke() : Int = 1
}
