// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 2 -> sentence 3
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 4 -> sentence 1
 * NUMBER: 6
 * DESCRIPTION: Star-imported extension callables
 */


// FILE: LibCase1.kt
// TESTCASE NUMBER: 1
package libPackage

/*public*/ private <!NOTHING_TO_INLINE!>inline<!> operator fun CharSequence.contains(regex: Regex): Boolean = regex.containsMatchIn(this)


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1
import libPackage.*

class Case() {

    fun foo() {
        "".<!DEBUG_INFO_CALL("fqName: kotlin.text.contains; typeCall: inline operator extension function")!>contains(Regex(""))<!>
    }
}
