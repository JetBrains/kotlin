// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 7
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Top-level non-extension functions: Implicitly imported callables
 */

// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1
import libPackageCase1.* //nothing to import, function emptyArray is private

fun case1() {
    <!DEBUG_INFO_CALL("fqName: kotlin.emptyArray; typeCall: inline function")!>emptyArray<Int>()<!>
}
// FILE: Lib.kt
package libPackageCase1

private fun <T> emptyArray(): Array<T> = TODO()

// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package testscase2

import libPackagecase2.* //nothing to import, emptyArray is private

class Case2 {

    fun case2() {
        <!DEBUG_INFO_CALL("fqName: kotlin.emptyArray; typeCall: inline function")!>emptyArray<Int>()<!>
    }
}

class A {
    operator fun <T>invoke(): T = TODO()
}

// FILE: Lib.kt
package libPackagecase2
import testscase2.*
private fun <T> emptyArray(): Array<T> = TODO()

private val Case2.emptyArray: A
    get() = A()

