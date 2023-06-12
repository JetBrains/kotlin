// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 4
 * NUMBER: 4
 * DESCRIPTION: Star-imported infix extension callables
 */

// FILE: Extensions1.kt
package libPackage

 operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my contains")
    return true
}
// FILE: Extensions2.kt

package sentence3

 operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope contains")
    return true
}

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.*


fun case1() {
    val regex = Regex("")
   "" <!INFIX_MODIFIER_REQUIRED!>contains<!>  regex
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPack
import libPackage.*

 operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope top level contains")
    return true
}

fun case2() {
    val regex = Regex("")
    "" <!INFIX_MODIFIER_REQUIRED!>contains<!>  regex
}
