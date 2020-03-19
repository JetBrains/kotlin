// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 11 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: set of star-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun String?.orEmpty(): String = "my Extension for $this"

// FILE: TestCase2.kt
package sentence3

import libPackage.*

// TESTCASE NUMBER: 1
fun case2(s: String?) {
    s.<!DEBUG_INFO_CALL("fqName: libPackage.orEmpty; typeCall: extension function")!>orEmpty()<!>
}
