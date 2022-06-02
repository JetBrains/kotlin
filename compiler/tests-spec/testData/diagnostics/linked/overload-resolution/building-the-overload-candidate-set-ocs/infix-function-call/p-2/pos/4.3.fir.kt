// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

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


fun case1() {
    val regex = Regex("")
    <!DEBUG_INFO_CALL("fqName: libPackage.contains; typeCall: infix operator extension function")!>"" contains  regex<!>
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.contains


fun case2() {
    infix operator fun CharSequence.contains(regex: Regex): Boolean {
        println("my local contains")
        return true
    }
    val regex = Regex("")
    <!DEBUG_INFO_CALL("fqName: sentence3.case2.contains; typeCall: infix operator extension function")!>"" contains  regex<!>
}
