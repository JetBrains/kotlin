// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE -EXTENSION_SHADOWED_BY_MEMBER -EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE
// SKIP_TXT


// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*
import libPackageCase1Explicit.listOf

class Case1() {
    fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

    fun case1() {
        fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

        <!DEBUG_INFO_CALL("fqName: testsCase1.Case1.listOf; typeCall: function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
    }
}
// FILE: Lib.kt
package libPackageCase1
import testsCase1.*

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib.kt
package libPackageCase1Explicit
import testsCase1.*

fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

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
    fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

    val listOf: A
        get() = A()

    fun case1() {
        fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

        <!DEBUG_INFO_CALL("fqName: testsCase2.Case2.listOf; typeCall: function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
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

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib.kt
package libPackageCase2Explicit
import testsCase2.*

val Case2.listOf: A
    get() = A()

fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase2
val Case2.listOf: A
    get() = A()

fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*
import libPackageCase3Explicit.listOf

class Case3() {
    fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T =
    { TODO() }): List<T> = TODO()

    val listOf: A
        get() = A()

    fun case1() {
        fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
    }
}

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}
// FILE: Lib.kt
// TESTCASE NUMBER: 3

package libPackageCase3
import testsCase3.*

val Case3.listOf: A
    get() = A()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib.kt
// TESTCASE NUMBER: 3

package libPackageCase3Explicit
import testsCase3.*

val Case3.listOf: A
    get() = A()

fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack.kt
// TESTCASE NUMBER: 3

package testsCase3
val Case3.listOf: A
    get() = A()

fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
