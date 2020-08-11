// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 4
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 6
 * NUMBER: 3
 * DESCRIPTION: Top-level non-extension functions named f: callables implicitly imported into the current file;
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package tests.case1

import lib.case1.*

interface I
class A : I
class B : I

fun case1(){
    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case1.A")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case1.A.<init>; typeCall: function")!>A()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case1.B")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case1.B.<init>; typeCall: function")!>B()<!>
}

// FILE: Lib.kt
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
class A : I
class B : I

fun case2(){
    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case2.A")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case2.A.<init>; typeCall: function")!>A()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case2.B")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case2.B.<init>; typeCall: function")!>B()<!>
}

// FILE: Lib.kt
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
class A : I
class B : I

fun case3(){
    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case3.A")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case3.A.<init>; typeCall: function")!>A()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("tests.case3.B")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: tests.case3.B.<init>; typeCall: function")!>B()<!>
}

// FILE: Lib.kt
package lib.case3

//fun A() : String = ""

object A {
    /*operator*/ fun invoke() : Int = 1
}
