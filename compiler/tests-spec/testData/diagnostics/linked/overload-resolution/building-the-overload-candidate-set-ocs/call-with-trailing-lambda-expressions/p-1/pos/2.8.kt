// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 8
 * DESCRIPTION: call-with-trailing-lambda-expressions,Explicit receiver:  Top-level non-extension functions: Callables explicitly imported into the current file
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*
import libPackageCase1Explicit.listOf

fun case1() {
    <!DEBUG_INFO_CALL("fqName: libPackageCase1Explicit.listOf; typeCall: function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
}

// FILE: Lib1.kt
package libPackageCase1

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib2.kt
package libPackageCase1Explicit

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack1.kt
package testsCase1

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*
import libPackageCase2Explicit.listOf

fun case2() {
    <!DEBUG_INFO_CALL("fqName: libPackageCase2Explicit.listOf; typeCall: function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
}

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}
// FILE: Lib3.kt
package libPackageCase2

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib4.kt
package libPackageCase2Explicit
import testsCase2.*

val listOf: A
    get() = A()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack2.kt
package testsCase2

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*
import libPackageCase3Explicit.listOf

fun case3() {
    <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
}

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}
// FILE: Lib5.kt
package libPackageCase3

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib6.kt
package libPackageCase3Explicit
import testsCase3.*

val listOf: A
    get() = A()

private fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack3.kt
package testsCase3

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

