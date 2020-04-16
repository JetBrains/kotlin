// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-278
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 2
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 5
 * DESCRIPTION: The overload candidate sets for each pair of implicit receivers: Implicitly imported extension callables
 */


// FILE: Lib.kt
package libPackageCase1
import testsCase1.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case1.emptyArray(): Array<T> = TODO()
// FILE: Lib.kt
package libPackageCase1Duplicate
import testsCase1.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case1.emptyArray(): Array<T> = TODO()

// FILE: Lib.kt
package libPackageCase1Explicit

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase1

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: TestCase.kt
package testsCase1
import libPackageCase1.*
import libPackageCase1Duplicate.*
import libPackageCase1Explicit.emptyArray

// TESTCASE NUMBER: 1
class Case1(){

    fun case1() {
        <!AMBIGUITY!>emptyArray<!><Int>()
    }
}
