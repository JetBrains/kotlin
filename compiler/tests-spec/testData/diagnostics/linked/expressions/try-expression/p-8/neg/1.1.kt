// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_VARIABLE -UNUSED_PARAMETER -FINAL_UPPER_BOUND
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: expressions, try-expression -> paragraph 8 -> sentence 1
 * PRIMARY LINKS: expressions, try-expression -> paragraph 9 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The type of the try-expression is the least upper bound of the types of the last expressions of the try body and the last expressions of all the catch blocks
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

fun throwExceptionA(b: Boolean) = run { if (b) throw ExcA() }

class ExcA() : Exception()
class ExcB() : Exception()

open class A<T>(var data: T) {
    fun foo(d: A<T>) {}
}

class B<T>(data: T) : A<T>(data)

fun case1() {
    val tryVal: B<String> =
    try <!TYPE_MISMATCH!>{
        throwExceptionA(false)
        A("")
    }<!> catch (e: Exception) {
        B("")
    }
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackCase2

fun throwExceptionA(b: Boolean) = run { if (b) throw ExcA() }

class ExcA() : Exception()
class ExcB() : Exception()

open class A<T>(var data: T) {
    fun foo(d: A<T>) {}
}

class B<T>(data: T) : A<T>(data)



fun case2() {
    val tryVal: A<String> =
    try {
        throwExceptionA(false)
        A("")
    } catch (e: Exception) <!TYPE_MISMATCH!>{
        null
    }<!>
}


// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
// ISSUES: KT-35494
package testPackCase3

fun throwExceptionA(b: Boolean) = run { if (b) throw ExcA() }

open class A<T>(var data: T) {
    fun foo(d: A<T>) {}
}

class B<T>(data: T) : A<T>(data)

fun case3() {
    val tryVal: A<Int> =
    try {
        throwExceptionA(false)
        A(2)
    } catch (e: ExcA) <!TYPE_MISMATCH!>{
        A(<!NULL_FOR_NONNULL_TYPE!>null<!>) //diag duplication
    }<!> catch (e: ExcB) <!TYPE_MISMATCH!>{
        B(<!NULL_FOR_NONNULL_TYPE!>null<!>) //diag duplication
    }<!>
}

class ExcA() : Exception()
class ExcB() : Exception()
