// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call
 * NUMBER: 4
 * DESCRIPTION: Local extension infix extension callables
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36786
 */


// TESTCASE NUMBER: 1, 2
// FILE: infixLib.kt
package libPackage

class C() {
    infix operator fun invoke(i: Int) {}
}

class B() {
    val barC: C = TODO()
    val B.barC: C
    get() = TODO()

    infix fun fooC(i: Int) = {}
    infix fun B.fooC(i: Int) =
    {}
}

infix fun B.fooC(i: Int) = {}
val B.barC: C
get() = TODO()


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * NOTE: should be resolved to member properties, others should be shadowed by member function
 */
package testPackInfix
import libPackage.C
import libPackage.B
import libPackage.*


fun case1() {
    class Case() {
        val B.barC: C
        get() = TODO()

        fun case() {
            val b = B()
            <!DEBUG_INFO_CALL("fqName: libPackage.B.fooC; typeCall: infix function")!>b fooC 3<!>
            <!DEBUG_INFO_CALL("fqName: libPackage.C.invoke; typeCall: variable&invoke")!>b barC 3<!>
        }
    }

    fun case() {
        val b = B()
        <!DEBUG_INFO_CALL("fqName: libPackage.B.fooC; typeCall: infix function")!>b fooC 3<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.C.invoke; typeCall: variable&invoke")!>b barC 3<!>
    }
}

// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 * NOTE: should be resolved to member properties, others should be shadowed by member property with invoke or by member function
 */
package testPackInfix
import libPackage.C
import libPackage.B
import libPackage.*
import libPackage.fooC
import libPackage.barC

/*should be shadowed by member function*/
infix fun B.fooC(i: Int) = {}
/*should be shadowed by member property with invoke*/
infix fun B.barC(i: Int) = {}
/*should be shadowed by member property*/
val B.barC: C
get() = TODO()

fun case2() {
    class Case() {
        /*should be shadowed by member property*/
        val B.barC: C
        get() = TODO()
        /*should be shadowed by member property with invoke*/
        infix fun B.barC(i: Int) = {}
        /*should be shadowed by member function*/
        infix fun B.fooC(i: Int) = {}
        fun case() {
            val b = B()
            <!DEBUG_INFO_CALL("fqName: libPackage.B.fooC; typeCall: infix function")!>b fooC 3<!>
            <!DEBUG_INFO_CALL("fqName: libPackage.C.invoke; typeCall: variable&invoke")!>b barC 3<!>
        }
    }

    fun case() {
        val b = B()
        <!DEBUG_INFO_CALL("fqName: libPackage.B.fooC; typeCall: infix function")!>b fooC 3<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.C.invoke; typeCall: variable&invoke")!>b barC 3<!>
    }
}
