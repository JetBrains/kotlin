// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 14 -> sentence 1
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 3
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 11 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 11 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION:  Both candidates are more applicable and few of them are non-parameterized
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

import testPackCase1.I2.Companion.foo
import testPackCase1.I1.Companion.foo
import testPackCase1.I3.Companion.foo
import testPackCase1.I4.Companion.foo

class Case1() : I2, I1, I3, I4 {

    fun test() {
       <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1)
    }
}

interface I2 {
    companion object {
        fun <T> foo(x: Int): Unit = print(1) // (1)
    }
}

interface I1 {
    companion object {
        fun foo(x: Int): String = "print(2)" // (2)
    }
}

interface I3 {
    companion object {
        fun foo(x: Short): Unit = print(3) // (3)
    }
}

interface I4 {
    companion object {
        fun foo(x: Int): Unit = print(4) // (4)
    }
}
