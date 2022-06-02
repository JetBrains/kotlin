// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*

fun case1() {
    <!DEBUG_INFO_CALL("fqName: testsCase1.listOf; typeCall: function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
}

// FILE: Lib1.kt
package libPackageCase1

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack1.kt
package testsCase1

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: testsCase2.listOf; typeCall: function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
}

// FILE: Lib2.kt
package libPackageCase2

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack2.kt
package testsCase2
import testsCase2.*

public val listOf: A
    get() = A()

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
}

// FILE: Lib3.kt
package libPackageCase3

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack3.kt
package testsCase3
import testsCase3.*

public val listOf: A
    get() = A()

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

private fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
