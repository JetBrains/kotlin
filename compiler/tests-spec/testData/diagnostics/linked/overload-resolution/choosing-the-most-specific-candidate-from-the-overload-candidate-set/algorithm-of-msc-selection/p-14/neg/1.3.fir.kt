// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

import testPackCase1.I2.Companion.foo
import testPackCase1.I1.Companion.foo

class Case2() : I2, I1{

    fun test(){
       <!AMBIGUITY!>foo<!>(1)
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
       <!AMBIGUITY!>foo<!>(1)
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
