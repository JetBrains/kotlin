// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 4
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 *
 * NUMBER: 5
 * DESCRIPTION: Star-imported infix extension callables
 */

// FILE: Extensions.kt
package libPackage

infix operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my contains")
    return true
}

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.*

fun case1() {
    val regex = Regex("")
    <!DEBUG_INFO_CALL("fqName: libPackage.contains; typeCall: infix operator extension function")!>"" contains  regex<!>
}

