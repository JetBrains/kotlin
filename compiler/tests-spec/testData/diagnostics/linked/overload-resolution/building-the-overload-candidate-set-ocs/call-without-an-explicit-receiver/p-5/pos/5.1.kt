// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 5
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Top-level non-extension functions: Callables declared in the same package
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libPackageCase1.*

fun case1() {
    <!DEBUG_INFO_CALL("fqName: testsCase1.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
}

// FILE: Lib1.kt
package libPackageCase1

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack1.kt
package testsCase1

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*

fun case2() {
    <!DEBUG_INFO_CALL("fqName: testsCase2.emptyArray; typeCall: function")!>emptyArray<Int>()<!>
}

// FILE: Lib2.kt
package libPackageCase2

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack2.kt
package testsCase2
import testsCase2.*

public val emptyArray: A
    get() = A()

class A {
    operator fun <T>invoke(): T = TODO()
}

public fun <T> emptyArray(): Array<T> = TODO()


// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*

fun case3() {
    <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>emptyArray<Int>()<!>
}

// FILE: Lib3.kt
package libPackageCase3

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack3.kt
package testsCase3
import testsCase3.*

public val emptyArray: A
    get() = A()

class A {
    operator fun <T>invoke(): T = TODO()
}

private fun <T> emptyArray(): Array<T> = TODO()

