// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 6
 * DESCRIPTION: call-with-trailing-lambda-expressions,Implicit receiver: set of implicitly-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun <T, R> T.let(block_: (T) -> R): R = TODO()

// FILE: TestCase.kt
package testCase1
import libPackage.*

// TESTCASE NUMBER: 1
fun case2(s: String) {
    s.<!DEBUG_INFO_CALL("fqName: libPackage.let; typeCall: extension function")!>let (block_ = { "" })<!>
    s.<!DEBUG_INFO_CALL("fqName: kotlin.let; typeCall: inline extension function")!>let (block = { "" })<!>
}
