// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * MAIN LINK:  overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 11
 * DESCRIPTION: call-with-trailing-lambda-expressions,Explicit receiver:  Top-level non-extension functions: Implicitly imported callables
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.* //nothing to import

fun case1() {
    <!DEBUG_INFO_CALL("fqName: kotlin.run; typeCall: inline function")!>run(block = { "ss" })<!>
}
// FILE: Lib1.kt
package libPackageCase1

private fun <R> run(vararg elements: R = TODO(), block: () -> R): R = TODO()

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testscase2

import libPackagecase2.* //nothing to import

class Case2 {

    fun case2() {
        <!DEBUG_INFO_CALL("fqName: kotlin.run; typeCall: inline extension function")!>run(block = { "ss" })<!>
    }
}

class A {
    fun <R> run(vararg elements: R = TODO(), block: () -> R): R = TODO()
}

// FILE: Lib2.kt
package libPackagecase2
import testscase2.*

private fun <R> run(vararg elements: R = TODO(), block: () -> R): R = TODO()

private val Case2.run: A
    get() = A()

