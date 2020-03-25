// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 3
 *
 * NUMBER: 19
 * DESCRIPTION: infix fun: Explicitly imported infix extension callables
 */

// FILE: Extensions.kt
package libPackage

infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

// FILE: Extensions.kt

package sentence3

infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.trim


fun case1() {
    <!DEBUG_INFO_CALL("fqName: libPackage.trim; typeCall: infix extension function")!>"" trim {true}<!>
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.trim


fun case2() {
    infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

    <!DEBUG_INFO_CALL("fqName: sentence3.case2.trim; typeCall: infix extension function")!>"" trim {true}<!>
}

