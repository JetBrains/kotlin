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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(1)<!>
        <!DEBUG_INFO_CALL("fqName: testPackCase1.I1.Companion.foo; typeCall: function")!>foo(1)<!>
    }
}

interface I2{
    companion object  {
        fun <T>foo(x: Int): Unit = print(1) // (1)
    }
}

interface  I1{
    companion object  {
        fun foo(x: Int): String = "print(2)" // (2)
    }
}
