// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 4
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 6
 * NUMBER: 4
 * DESCRIPTION: Top-level non-extension functions named f: callables implicitly imported into the current file;
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package tests.case1

import lib.case1.*

interface I
//class A : I
class B : I

fun case1(){
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case1.A; typeCall: function")!>A()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case1.B")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case1.B.B; typeCall: function")!>B()<!>
}

// FILE: Lib1.kt
package lib.case1

fun A() : String = ""

object A {
    operator fun invoke() : Int = 1
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package tests.case2

import lib.case2.*

interface I
//class A : I
class B : I

fun case2(){
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case2.A.invoke; typeCall: variable&invoke")!>A()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case2.B")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case2.B.B; typeCall: function")!>B()<!>
}

// FILE: Lib2.kt
package lib.case2

//fun A() : String = ""

object A {
    operator fun invoke() : Int = 1
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3

package tests.case3

import lib.case3.*

interface I
//class A : I
class B : I

fun case3(){

    A.invoke()
    A.<!DEBUG_INFO_CALL("fqName: lib.case3.A.invoke; typeCall: function")!>invoke()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case3.B")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case3.B.B; typeCall: function")!>B()<!>
}

// FILE: Lib3.kt
package lib.case3

//fun A() : String = ""

object A {
    /*operator*/ fun invoke() : Int = 1
}
