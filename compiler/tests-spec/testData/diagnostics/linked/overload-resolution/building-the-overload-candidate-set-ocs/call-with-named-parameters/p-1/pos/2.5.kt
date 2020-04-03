// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-280
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 1 -> sentence 2
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: Implicit receiver: set of star-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun String.padEnd(lengthParam: Int, padChar: Char = ' '): String = TODO()

// FILE: TestCase2.kt
package sentence3

import libPackage.*

// TESTCASE NUMBER: 1
fun case2(s: String) {
    s.<!DEBUG_INFO_CALL("fqName: libPackage.padEnd; typeCall: extension function")!>padEnd(lengthParam = 5)<!>
}
