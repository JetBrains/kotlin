// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libPackageCase1.*
import libPackageCase1Explicit.emptyArray

<!CONFLICTING_OVERLOADS!>fun <T> Case1.emptyArray(): Array<T><!> = TODO()

class Case1(){

    fun case1() {
        emptyArray<Int>()
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
        emptyArray<Int>()
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
