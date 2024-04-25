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
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 2
 *
 * NUMBER: 2
 * DESCRIPTION: Local extension infix extension callables
 */

// FILE: Extensions1.kt
package libPackage

infix operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my contains")
    return true
}
// FILE: Extensions2.kt

package sentence3

infix operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope contains")
    return true
}

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.contains

class Case1() {
    infix operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local class scope contains")
        return true
    }

    fun case1() {
        val regex = Regex("")
        <!DEBUG_INFO_CALL("fqName: sentence3.Case1.contains; typeCall: infix operator extension function")!>"" contains  regex<!>
    }
}
// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.contains

interface Case2 {
    infix operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local interface scope contains")
        return true
    }

    fun case2() {
        val regex = Regex("")
        <!DEBUG_INFO_CALL("fqName: sentence3.Case2.contains; typeCall: infix operator extension function")!>"" contains  regex<!>
    }
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testPack
import libPackage.contains

infix operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope top level contains")
    return true
}

fun case3() {
    infix operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local contains")
        return true
    }

    val regex = Regex("")
    <!DEBUG_INFO_CALL("fqName: testPack.case3.contains; typeCall: infix operator extension function")!>"" contains  regex<!>
}

// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackNew
import libPackage.contains

infix operator fun CharSequence.contains(regex: Regex): Boolean {
    println("my package scope top level contains")
    return true
}

fun case4() {

    infix operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local contains")
        return true
    }

    fun subfun() {

        infix operator fun CharSequence.contains(regex: Regex): Boolean {
            println("my local contains")
            return true
        }

        val regex = Regex("")
        <!DEBUG_INFO_CALL("fqName: testPackNew.case4.subfun.contains; typeCall: infix operator extension function")!>"" contains  regex<!>

    }
}
