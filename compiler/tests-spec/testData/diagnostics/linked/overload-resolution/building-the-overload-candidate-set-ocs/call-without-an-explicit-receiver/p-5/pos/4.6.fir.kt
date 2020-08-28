// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package tests.case1

import lib.case1.b.C as B
import lib.case1.a.C as A

interface I
class A : I
class B : I

fun case1(){
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case1.a.C; typeCall: function")!>A()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case1.b.C; typeCall: function")!>B()<!>
}

// FILE: Lib.kt
package lib.case1.a

fun C() : String = ""

object C {
    operator fun invoke() : Int = 1
}

// FILE: Lib.kt
package lib.case1.b

fun C() : String = ""

object C {
    operator fun invoke() : Int = 1
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package tests.case2

import lib.case2.a.C as A
import lib.case2.b.C as B

interface I
class A : I
class B : I

fun case2(){
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case2.a.C.invoke; typeCall: variable&invoke")!>A()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case2.b.C.invoke; typeCall: variable&invoke")!>B()<!>
}

// FILE: Lib.kt
package lib.case2.a

//fun C() : String = ""

object C {
    operator fun invoke() : Int = 1
}
// FILE: Lib.kt
package lib.case2.b

//fun C() : String = ""

object C {
    operator fun invoke() : Int = 1
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3

package tests.case3

import lib.case3.b.C as B
import lib.case3.a.C as A

interface I
class A : I
class B : I

fun case3(){
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>A()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case3.a.C.invoke; typeCall: variable&invoke")!>A()<!>

    A.invoke()
    A.<!DEBUG_INFO_CALL("fqName: lib.case3.a.C.invoke; typeCall: function")!>invoke()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>B()<!>
    <!DEBUG_INFO_CALL("fqName: lib.case3.b.C.invoke; typeCall: variable&invoke")!>B()<!>

    B.invoke()
    B.<!DEBUG_INFO_CALL("fqName: lib.case3.b.C.invoke; typeCall: function")!>invoke()<!>
}

// FILE: Lib.kt
package lib.case3.a

//fun B() : String = ""

object C {
    /*operator*/ fun invoke() : Int = 1
}
// FILE: Lib.kt
package lib.case3.b

//fun C() : String = ""

object C {
    /*operator*/ fun invoke() : Int = 1
}
