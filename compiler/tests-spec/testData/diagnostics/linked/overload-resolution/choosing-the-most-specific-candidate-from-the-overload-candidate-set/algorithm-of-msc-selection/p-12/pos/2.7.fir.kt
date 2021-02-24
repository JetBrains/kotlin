// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT



// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

import testPackCase1.I1.Companion.invoke
import testPackCase1.I2.Companion.invoke

class Case1() : I2, I1 {

    fun case() {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>invoke(1)<!>
        <!DEBUG_INFO_CALL("fqName: testPackCase1.I1.Companion.invoke; typeCall: operator function")!>invoke(1)<!>
    }
}

interface I2{
    companion object  {
        operator fun invoke(x: Short): Unit = print(3) // (3)
    }
}
interface I1{
    companion object  {
        operator fun invoke(x: Int): String = "print(3)" // (3)
    }
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

class Case2() : I3 {
    operator fun invoke(x: Short): Unit = print(3) // (1)
    companion object {
        operator fun invoke(x: Int): Unit = print(3) // (2)
    }

    fun case() {
        <!DEBUG_INFO_CALL("fqName: I3.invoke; typeCall: operator function")!>invoke(1)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>invoke(1)<!>
    }
}

interface I3 {
    operator fun invoke(x: Int): String = "print(3)" // (3)
}
