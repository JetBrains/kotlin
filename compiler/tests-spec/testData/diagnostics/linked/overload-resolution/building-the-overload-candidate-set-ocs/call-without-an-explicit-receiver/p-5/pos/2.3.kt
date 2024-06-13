// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 3
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: The overload candidate sets for each pair of implicit receivers: Explicitly imported extension callables
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package testsCase1
import libPackageCase1.*
import libPackageCase1Explicit.emptyArray

class Case1(){

    fun case1() {
        <!DEBUG_INFO_CALL("fqName: libPackageCase1Explicit.emptyArray; typeCall: extension function")!>emptyArray<Int>()<!>
    }
}

// FILE: Lib1.kt
package libPackageCase1
import testsCase1.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case1.emptyArray(): Array<T> = TODO()

// FILE: Lib2.kt
package libPackageCase1Explicit
import testsCase1.*

fun <T> Case1.emptyArray(): Array<T> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack1.kt
package testsCase1
fun <T> Case1.emptyArray(): Array<T> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()


// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package testsCase2
import libPackageCase2.*
import libPackageCase2Explicit.emptyArray

class Case2(){

    fun case1() {
        <!DEBUG_INFO_CALL("fqName: libPackageCase2Explicit.emptyArray; typeCall: extension function")!>emptyArray<Int>()<!>
    }
}

class A {
    operator fun <T>invoke(): T = TODO()
}

// FILE: Lib3.kt
package libPackageCase2
import testsCase2.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case2.emptyArray(): Array<T> = TODO()

// FILE: Lib4.kt
package libPackageCase2Explicit
import testsCase2.*

val Case2.emptyArray: A
    get() = A()

fun <T> Case2.emptyArray(): Array<T> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack2.kt
package testsCase2
fun <T> Case2.emptyArray(): Array<T> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()



// FILE: TestCase3.kt
// TESTCASE NUMBER: 3

package testsCase3
import libPackageCase3.*
import libPackageCase3Explicit.emptyArray

class Case3(){

    fun case1() {
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>emptyArray<Int>()<!>
    }
}

class A {
    operator fun <T>invoke(): T = TODO()
}

// FILE: Lib5.kt
package libPackageCase3
import testsCase3.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case3.emptyArray(): Array<T> = TODO()

// FILE: Lib6.kt
package libPackageCase3Explicit
import testsCase3.*

val Case3.emptyArray: A
    get() = A()

private fun <T> Case3.emptyArray(): Array<T> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack3.kt
package testsCase3
fun <T> Case3.emptyArray(): Array<T> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()

