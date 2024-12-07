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
 * NUMBER: 2
 * DESCRIPTION: call-with-trailing-lambda-expressions,Implicit receiver: sets of local, explicitly imported, declared in the package scope and star-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun <T, R> T.let(block: (T) -> R): R = TODO()

// FILE: TestCase1.kt

package sentence3

import libPackage.*
import libPackage.let

private fun <T, R> T.let(block: (T) -> R): R = TODO()

// TESTCASE NUMBER: 1
class Case1() {
    fun <T, R> T.let(block: (T) -> R): R = TODO()
    fun case1(s: String) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.let; typeCall: extension function")!>let (block = { "" })<!>
    }
}

// FILE: TestCase2.kt
package sentence3
import libPackage.*

//import libPackage.orEmpty

private fun <T, R> T.let(block: (T) -> R): R = TODO()

// TESTCASE NUMBER: 2
class Case2() {
    fun <T, R> T.let(block: (T) -> R): R = TODO()
    fun case2(s: String) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.let; typeCall: extension function")!>let (block = { "" })<!>
    }
}

// TESTCASE NUMBER: 3
class Case3() {
    fun <T, R> T.let(block: (T) -> R): R = TODO()
    fun case3(s: String) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.let; typeCall: extension function")!>let (block = { "" })<!>
        fun innerFirst(s: String?) {
            fun <T, R> T.let(block: (T) -> R): R = TODO()
            s?.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.let; typeCall: extension function")!>let (block = { "" })<!>
        }

        fun innerSecond() {
            fun <T, R> T.let(block: (T) -> R): R = TODO()
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.let; typeCall: extension function")!>let (block = { "" })<!>
        }
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.let; typeCall: extension function")!>let (block = { "" })<!>
    }
}
