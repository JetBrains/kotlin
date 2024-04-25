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
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 2
 *
 * NUMBER: 18
 * DESCRIPTION: Infix fun: Local extension infix extension callables
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
import libPackage.trim

class Case1() {
    infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

    fun case1() {
        <!DEBUG_INFO_CALL("fqName: sentence3.Case1.trim; typeCall: infix extension function")!>"" trim {true}<!>
    }
}
// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.trim

interface Case2 {
    infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

    fun case2() {
        <!DEBUG_INFO_CALL("fqName: sentence3.Case2.trim; typeCall: infix extension function")!>"" trim {true}<!>
    }
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testPack
import libPackage.trim

infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()
fun case3() {
    infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

    <!DEBUG_INFO_CALL("fqName: testPack.case3.trim; typeCall: infix extension function")!>"" trim {true}<!>
}

// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackNew
import libPackage.trim

infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

fun case4() {

    infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

    fun subfun() {

        infix fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence = TODO()

        <!DEBUG_INFO_CALL("fqName: testPackNew.case4.subfun.trim; typeCall: infix extension function")!>"" trim {true}<!>

    }
}
