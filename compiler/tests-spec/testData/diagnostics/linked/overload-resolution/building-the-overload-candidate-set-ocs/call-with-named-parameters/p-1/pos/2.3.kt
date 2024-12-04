// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-280
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Implicit receiver: sets of explicitly imported, declared in the package scope and star-imported extension callables
 */

// FILE: Extensions1.kt
package libPackageExplicit

public fun String.padEnd(lengthParam: Int, padChar: Char = ' '): String = TODO()

// FILE: Extensions2.kt
package libPackage

public fun String.padEnd(lengthParam: Int, padChar: Char = ' '): String = TODO()

// FILE: TestCase1.kt
package tests

import libPackage.*
import libPackageExplicit.padEnd

// TESTCASE NUMBER: 1
fun case1(s: String) {
    s.<!DEBUG_INFO_CALL("fqName: libPackageExplicit.padEnd; typeCall: extension function")!>padEnd(lengthParam = 5)<!>
}

