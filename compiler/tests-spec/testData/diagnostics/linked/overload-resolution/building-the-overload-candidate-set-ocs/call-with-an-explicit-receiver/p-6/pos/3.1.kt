// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK:overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 3
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 4
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 11 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: sets of explicitly imported, declared in the package scope and star-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun String?.orEmpty(): String = "my Extension for $this"
/* a call with trailing lambda expression */
public fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

// FILE: TestCase1.kt
package sentence3

import libPackage.*
import libPackage.orEmpty
import libPackage.funWithTrailingLambda


private fun String?.orEmpty(): String = "my top level extension for $this"
/* a call with trailing lambda expression */
public fun String?.funWithTrailingLambda( x : Any? = null, body : ()-> String = {""} ): String = body()

// TESTCASE NUMBER: 1
fun case1(s: String?) {
    s.<!DEBUG_INFO_CALL("fqName: libPackage.orEmpty; typeCall: extension function")!>orEmpty()<!>

    s.<!DEBUG_INFO_CALL("fqName: libPackage.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda { "ss" }<!>
    s.<!DEBUG_INFO_CALL("fqName: libPackage.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (x= 1) { "ss" }<!>
    s.<!DEBUG_INFO_CALL("fqName: libPackage.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" })<!>
    s.<!DEBUG_INFO_CALL("fqName: libPackage.funWithTrailingLambda; typeCall: extension function")!>funWithTrailingLambda (body = { "ss" }, x = '1')<!>
}

