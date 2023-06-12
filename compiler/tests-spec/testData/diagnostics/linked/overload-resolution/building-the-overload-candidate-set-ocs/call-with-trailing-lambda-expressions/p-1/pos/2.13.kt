// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE -EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-300
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-named-parameters -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 13
 * DESCRIPTION: call-with-trailing-lambda-expressions,Explicit receiver: The overload candidate sets for each pair of implicit receivers: Local extension callables
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

import libPackageCase1.*
import libPackageCase1Explicit.listOf

class Case1() {

    fun case1_0() {
        fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

        <!DEBUG_INFO_CALL("fqName: testsCase1.Case1.case1_0.listOf; typeCall: extension function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>

        fun case1_1() {
            fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
            <!DEBUG_INFO_CALL("fqName: testsCase1.Case1.case1_0.case1_1.listOf; typeCall: extension function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
        }
    }

}

// FILE: Lib1.kt
package libPackageCase1
import testsCase1.*

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib2.kt
package libPackageCase1Explicit
import testsCase1.*

fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack1.kt
package testsCase1
fun <T> Case1.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase2.kt

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37179
 */
package testsCase2
import libPackageCase2.*
import libPackageCase2Explicit.listOf

class Case2() {

    fun case1_0() {
        fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
        <!DEBUG_INFO_CALL("fqName: testsCase2.Case2.case1_0.listOf; typeCall: extension function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>

        fun case1_1() {
            fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
            <!DEBUG_INFO_CALL("fqName: testsCase2.Case2.case1_0.case1_1.listOf; typeCall: extension function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
        }
    }

    val Case2.listOf: A
        get() = A()

    fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T =
    { TODO() }): List<T> = TODO()

    fun case2_0() {
        <!DEBUG_INFO_CALL("fqName: testsCase2.Case2.listOf; typeCall: extension function")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
    }

}

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

// FILE: Lib3.kt
// TESTCASE NUMBER: 2
package libPackageCase2
import testsCase2.*

val Case2.listOf: A
    get() = A()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib4.kt
// TESTCASE NUMBER: 2

package libPackageCase2Explicit
import testsCase2.*

fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack2.kt
// TESTCASE NUMBER: 2

package testsCase2
fun <T> Case2.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()


// FILE: TestCase3.kt

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37179
 */
package testsCase3
import libPackageCase3.*
import libPackageCase3Explicit.listOf

class Case3() {

    fun case1_0() {
        //fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements1 = arrayOf(1), body = { "" })<!>

        fun case1_1() {
            //  fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
            <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
        }
    }

    val Case3.listOf: A
        get() = A()

    //fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

    fun case2_0() {
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>listOf(elements1 = arrayOf(1), body = { "" })<!>
    }

}

class A {
    operator fun <T> invoke(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
}

// FILE: Lib5.kt
// TESTCASE NUMBER: 3
package libPackageCase3
import testsCase3.*

val Case3.listOf: A
    get() = A()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()
fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: Lib6.kt
// TESTCASE NUMBER: 3

package libPackageCase3Explicit
import testsCase3.*

fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

// FILE: LibtestsPack3.kt
// TESTCASE NUMBER: 3
package testsCase3
fun <T> Case3.listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

public fun <T> listOf(vararg elements1: T = TODO(), body: () -> T = { TODO() }): List<T> = TODO()

