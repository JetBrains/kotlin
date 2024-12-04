// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-280
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Implicit receiver: sets of local, explicitly imported, declared in the package scope and star-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()

// FILE: TestCase1.kt

package sentence3

import libPackage.*
import libPackage.padEnd

private fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()

// TESTCASE NUMBER: 1
class Case1() {
    fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()
    fun case1(s: String) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case1.padEnd; typeCall: extension function")!>padEnd(length = 3)<!>
    }
}

// FILE: TestCase2.kt
package sentence3
import libPackage.*

//import libPackage.orEmpty

private fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()

// TESTCASE NUMBER: 2
class Case2() {
    fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()
    fun case2(s: String) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case2.padEnd; typeCall: extension function")!>padEnd(length = 3)<!>
    }
}

// TESTCASE NUMBER: 3
class Case3() {
    public fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()
    fun case3(s: String) {
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.padEnd; typeCall: extension function")!>padEnd(length = 3)<!>
        fun innerFirst(s: String?) {
            fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()
            s?.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerFirst.padEnd; typeCall: extension function")!>padEnd(length = 3)<!>
        }

        fun innerSecond() {
            fun String.padEnd(length: Int, padChar: Char = ' '): String = TODO()
            s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.case3.innerSecond.padEnd; typeCall: extension function")!>padEnd(length = 3)<!>
        }
        s.<!DEBUG_INFO_CALL("fqName: sentence3.Case3.padEnd; typeCall: extension function")!>padEnd(length = 3)<!>
    }
}
