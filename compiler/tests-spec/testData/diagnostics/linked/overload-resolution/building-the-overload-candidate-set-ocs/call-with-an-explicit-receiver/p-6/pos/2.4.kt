// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * PLACE:  overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 2
 * RELEVANT PLACES:  overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 3
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 4
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 11 -> sentence 1
 * overload-resolution, receivers -> paragraph 5 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: sets of local, explicitly imported, declared in the package scope and star-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun String?.orEmpty(): String = "my Extension for $this"

// FILE: TestCase1.kt

package sentence3

import libPackage.*
import libPackage.orEmpty

private fun String?.orEmpty(): String = "my top-level (pack scope) Extension for $this"

// TESTCASE NUMBER: 1
class Case1() {
    fun String?.orEmpty(): String = "my local extension for $this"
    fun case1(s: String?) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.orEmpty; typeCall: extension function")!>orEmpty()<!>
    }
}

// FILE: TestCase2.kt
package sentence3
import libPackage.*
//import libPackage.orEmpty

private fun String?.orEmpty(): String = "my top-level (pack scope) Extension for $this"

// TESTCASE NUMBER: 2
class Case2() {
    fun String?.orEmpty(): String = "my local extension for $this"
    fun case2(s: String?) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.orEmpty; typeCall: extension function")!>orEmpty()<!>
    }
}

// TESTCASE NUMBER: 3
class Case3() {
    fun String?.orEmpty(): String = "my local extension for $this"
    fun case3(s: String?) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.orEmpty; typeCall: extension function")!>orEmpty()<!>
        fun innerFirst(s: String?){
            fun String?.orEmpty(): String = "my local inner extension for $this"
            <!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.s; typeCall: typeCall is unknown")!>s<!>?.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.orEmpty; typeCall: extension function")!>orEmpty()<!>
        }
        fun innerSecond(){
            fun String?.orEmpty(): String = "my local inner extension for $this"
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.orEmpty; typeCall: extension function")!>orEmpty()<!>
        }
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.orEmpty; typeCall: extension function")!>orEmpty()<!>
    }
}
