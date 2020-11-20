// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * MAIN LINK:  overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 9
 * DESCRIPTION: call-with-trailing-lambda-expressions,Explicit receiver:  Top-level non-extension functions: Callables declared in the same package
 */

// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*

fun case1() {
    <!DEBUG_INFO_CALL("fqName: testsCase1.listOf; typeCall: function")!>listOf(<!CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS!>elements1 = arrayOf(1)<!>, body = { "" })<!>
}

// FILE: Lib.kt
package libPackageCase1

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase1

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: testsCase2.listOf; typeCall: function")!>listOf(<!CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS!>elements1 = arrayOf(1)<!>, body = { "" })<!>
}

// FILE: Lib.kt
package libPackageCase2

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase2
import testsCase2.*

public val listOf: A
    get() = A()

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(<!CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS!>elements1 = arrayOf(1)<!>, body = { "" })<!>
}

// FILE: Lib.kt
package libPackageCase3

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase3
import testsCase3.*

public val listOf: A
    get() = A()

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

private fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

