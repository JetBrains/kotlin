// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: call-with-trailing-lambda-expressions,Implicit receiver: sets of explicitly imported, declared in the package scope and star-imported extension callables
 */

// FILE: Extensions.kt
package libPackageExplicit

public fun <T, R> T.let(block: (T) -> R): R = TODO()

// FILE: Extensions.kt
package libPackage

public fun <T, R> T.let(block: (T) -> R): R = TODO()

// FILE: TestCase1.kt
package tests

import libPackage.*
import libPackageExplicit.let

// TESTCASE NUMBER: 1
fun case1(s: String) {
    s.<!DEBUG_INFO_CALL("fqName: libPackageExplicit.let; typeCall: extension function")!>let (block = { "" })<!>
}

