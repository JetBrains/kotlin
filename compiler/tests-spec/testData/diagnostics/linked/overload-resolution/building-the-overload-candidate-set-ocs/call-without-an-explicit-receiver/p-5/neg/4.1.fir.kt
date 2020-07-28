// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT



// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package tests.case1

import lib.case1.*

interface I
class A : I
class B : I

fun case1(){
    A.<!UNRESOLVED_REFERENCE!>invoke<!>()
}

// FILE: Lib.kt
package lib.case1

//fun A() : String = ""

object A {
    /*operator*/ fun invoke() : Int = 1
}
