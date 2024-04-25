// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 6
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 11 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: set of implicitly-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

private fun String?.orEmpty(): String = "my private Extension for $this"

// FILE: TestCase2.kt
package sentence3
import libPackage.* //nothing to import, extension is private

// TESTCASE NUMBER: 1
fun case2(s: String) {
    s.<!DEBUG_INFO_CALL("fqName: kotlin.text.orEmpty; typeCall: inline extension function")!>orEmpty()<!>
}
