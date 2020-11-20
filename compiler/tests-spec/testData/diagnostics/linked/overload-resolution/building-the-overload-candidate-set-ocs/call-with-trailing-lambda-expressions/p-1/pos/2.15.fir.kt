// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT


// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*
import libPackageCase1Explicit.listOf

class Case1() {

    fun case1() {
        <!DEBUG_INFO_CALL("fqName: testsCase1.listOf; typeCall: extension function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
    }
}

// FILE: Lib.kt
package libPackageCase1
import testsCase1.*

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib.kt
package libPackageCase1Explicit

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase1
fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*
import libPackageCase2Explicit.listOf

class Case2() {

    fun case1() {
        <!DEBUG_INFO_CALL("fqName: testsCase2.listOf; typeCall: extension function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
    }
}

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

// FILE: Lib.kt
package libPackageCase2
import testsCase2.*

val Case2.listOf: A
    get() = A()

fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib.kt
package libPackageCase2Explicit

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase2
fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
val Case2.listOf: A
    get() = A()

// FILE: TestCase.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*
import libPackageCase3Explicit.listOf

class Case3() {

    fun case1() {
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
    }
}

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

// FILE: Lib.kt
package libPackageCase3
import testsCase3.*

fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib.kt
package libPackageCase3Explicit

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase3

val Case3.listOf: A
    get() = A()

private fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
