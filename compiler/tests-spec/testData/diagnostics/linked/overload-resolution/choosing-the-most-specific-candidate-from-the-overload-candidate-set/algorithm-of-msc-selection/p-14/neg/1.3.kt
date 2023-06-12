// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 14 -> sentence 1
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 3
 * NUMBER: 3
 * DESCRIPTION: call with explicit receiver: different built-in integer types and both of them are kotlin.Int
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

import testPackCase1.I2.Companion.foo
import testPackCase1.I1.Companion.foo

class Case2() : I2, I1{

    fun test(){
       <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1)
    }
}

interface I2{
    companion object  {
        fun foo(x: Int): Unit = print(1) // (1)
    }
}

interface  I1{
    companion object  {
        fun foo(x: Int): String = "print(2)" // (2)
    }
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackCase2

import testPackCase2.I2.Companion.foo
import testPackCase2.I1.Companion.foo

class Case2() : I2, I1{

    fun test(){
       <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1)
    }
}

interface I2{
    companion object  {
        fun <T>foo(x: Int): Unit = print(1) // (1)
    }
}

interface  I1{
    companion object  {
        fun <R>foo(x: Int): String = "print(2)" // (2)
    }
}
