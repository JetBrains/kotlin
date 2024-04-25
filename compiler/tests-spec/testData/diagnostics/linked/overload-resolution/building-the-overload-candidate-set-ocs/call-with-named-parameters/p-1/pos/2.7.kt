// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-280
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * NUMBER: 7
 * DESCRIPTION: Implicit receiver: set of implicitly-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

private fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()

// FILE: TestCase.kt
package testCase1
import libPackage.*

// TESTCASE NUMBER: 1
fun case2(s: String) {
    s.<!DEBUG_INFO_CALL("fqName: kotlin.text.padEnd; typeCall: extension function")!>padEnd(length = 5)<!>
}
