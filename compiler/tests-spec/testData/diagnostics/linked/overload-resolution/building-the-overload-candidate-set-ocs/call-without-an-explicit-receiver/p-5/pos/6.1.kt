// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 6
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Top-level non-extension functions: Callables star-imported into the current file;
 */

// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*

fun case1() {
    <!DEBUG_INFO_CALL("fqName: libPackageCase1.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
}
// FILE: Lib.kt
package libPackageCase1

public fun <T> emptyArray(): Array<T> = TODO()


// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: libPackageCase2.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
}


class A {
    operator fun <T> invoke(): T = TODO()
}

// FILE: Lib.kt
package libPackageCase2
import testsCase2.*

public fun <T> emptyArray(): Array<T> = TODO()

val emptyArray: A
    get() = A()

// FILE: TestCase.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>emptyArray<Int>()<!>
}


class A {
    operator fun <T> invoke(): T = TODO()
}

// FILE: Lib.kt
package libPackageCase3
import testsCase3.*

private fun <T> emptyArray(): Array<T> = TODO()

val emptyArray: A
    get() = A()