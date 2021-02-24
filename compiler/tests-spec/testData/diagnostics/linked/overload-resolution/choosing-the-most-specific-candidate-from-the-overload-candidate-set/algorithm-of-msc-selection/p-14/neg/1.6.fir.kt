// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

import testPackCase1.I2.Companion.foo
import testPackCase1.I1.Companion.foo
import testPackCase1.I3.Companion.foo
import testPackCase1.I4.Companion.foo

class Case1() : I2, I1, I3, I4  {

    fun test() {
       <!AMBIGUITY!>foo<!>(1)
    }
}

interface I2 {
    companion object {
        fun <T> foo(x: Int, y: Any): Unit = print(1) // (1)
    }
}

interface I1 {
    companion object {
        fun foo(x: Int,  z: Any = "", vararg y: Any): String = "print(2)" // (2)
    }
}

interface I3 {
    companion object {
        fun foo(x: Short,  z: Any = "", vararg y: Any): Unit = print(3) // (3)
    }
}

interface I4 {
    companion object {
        fun foo(x: Int,  z: Any = "", vararg y: Any): Unit = print(4) // (4)
    }
}
