// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-280
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Implicit receiver: sets of declared in the package scope and star-imported extension callables
 */

// FILE: Extensions.kt
package libPackage

public fun String.padEnd(lengthParam: Int, padChar: Char = ' '): String = TODO()

// FILE: TestCase2.kt
package tests

import libPackage.*

public fun String.padEnd(lengthParam: Int, padChar: Char = ' '): String = TODO()

// TESTCASE NUMBER: 1
fun case2(s: String) {
    s.<!DEBUG_INFO_CALL("fqName: tests.padEnd; typeCall: extension function")!>padEnd(lengthParam = 5)<!>
}

