// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 4
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * NUMBER: 5
 * DESCRIPTION: Star-imported extension callable only
 */

// FILE: Extensions.kt
package libPackage

private infix operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my contains")
    return true
}

// FILE: TestCase2.kt
package sentence3
import libPackage.* //nothing to import, extension is private

// TESTCASE NUMBER: 1
fun case1() {
    val regex = Regex("")
    "" <!INFIX_MODIFIER_REQUIRED!>contains<!> regex
}
