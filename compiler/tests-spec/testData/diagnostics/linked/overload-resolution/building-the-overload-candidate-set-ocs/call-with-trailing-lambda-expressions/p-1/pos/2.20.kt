// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 4
 *
 * NUMBER: 20
 * DESCRIPTION: infix fun: Star-imported infix extension callables
 */

// FILE: Extensions1.kt
package libPackage

infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

// FILE: Extensions2.kt

package sentence3

infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.*


fun case1() {
    val regex = Regex("")
    <!DEBUG_INFO_CALL("fqName: sentence3.trim; typeCall: infix extension function")!>"" trim {true}<!>
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPack
import libPackage.*

infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()


fun case2() {
    val regex = Regex("")
    <!DEBUG_INFO_CALL("fqName: testPack.trim; typeCall: infix extension function")!>"" trim {true}<!>
}
