// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*

fun case1() {
    <!DEBUG_INFO_CALL("fqName: libPackageCase1.listOf; typeCall: function")!>listOf(elements = arrayOf(1), body = { "" })<!>
}
// FILE: Lib.kt
package libPackageCase1

fun <T> listOf(vararg elements: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: libPackageCase2.listOf; typeCall: function")!>listOf(elements = arrayOf(1), body = { "" })<!>
}


class A {
    operator fun <T> invoke(): T = TODO()
}

// FILE: Lib.kt
package libPackageCase2
import testsCase2.*

fun <T> listOf(vararg elements: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
val listOf: A
    get() = A()

// FILE: TestCase.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements = arrayOf(1), body = { "" })<!>
}


class A {
    operator fun <T> invoke(vararg elements: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

// FILE: Lib.kt
package libPackageCase3
import testsCase3.*

private fun <T> listOf(vararg elements: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

val listOf: A
    get() = A()
