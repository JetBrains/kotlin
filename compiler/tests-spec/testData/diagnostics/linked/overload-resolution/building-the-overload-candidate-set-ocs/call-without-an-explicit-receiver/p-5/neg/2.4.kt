// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-278
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 2
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 4
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 1 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION: The overload candidate sets for each pair of implicit receivers: declared in the package scope extension callables
 */



// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libPackageCase1.*
import libPackageCase1Explicit.emptyArray

<!CONFLICTING_OVERLOADS!>fun <T> Case1.emptyArray(): Array<T><!> = TODO()

class Case1(){

    fun case1() {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>emptyArray<!><Int>()
    }
}


// FILE: Lib1.kt
package libPackageCase1
import testsCase1.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case1.emptyArray(): Array<T> = TODO()

// FILE: Lib2.kt
package libPackageCase1Explicit

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack1.kt
// TESTCASE NUMBER: 1
package testsCase1
<!CONFLICTING_OVERLOADS!>fun <T> Case1.emptyArray(): Array<T><!> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()




// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*
import libPackageCase2Explicit.emptyArray

<!CONFLICTING_OVERLOADS!>fun <T> Case2.emptyArray(): Array<T><!> = TODO()

class Case2(){

    fun case1() {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>emptyArray<!><Int>()
    }
}

val Case2.emptyArray: A
    get() = A()

class A {
    operator fun invoke(): Unit = TODO()
}

// FILE: Lib3.kt
package libPackageCase2
import testsCase2.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case2.emptyArray(): Array<T> = TODO()

// FILE: Lib4.kt
package libPackageCase2Explicit

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack2.kt
// TESTCASE NUMBER: 2
package testsCase2
<!CONFLICTING_OVERLOADS!>fun <T> Case2.emptyArray(): Array<T><!> = TODO()

public fun <T> emptyArray(): Array<T> = TODO()



