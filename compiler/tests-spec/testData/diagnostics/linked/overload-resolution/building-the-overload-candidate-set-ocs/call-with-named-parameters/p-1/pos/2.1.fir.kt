// !DIAGNOSTICS: -UNUSED_VARIABLE -UNREACHABLE_CODE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -IMPLICIT_NOTHING_RETURN_TYPE -DEBUG_INFO_LEAKING_THIS -EXTENSION_SHADOWED_BY_MEMBER
// SKIP_TXT

// TESTCASE NUMBER: 1, 2, 3, 4, 5

// FILE: Marker.kt
package libPackage

class Marker {
    fun foo(y: Int) = TODO()
    val foo: Marker = this
    operator fun invoke(x: Int) =  TODO()
}

// FILE: Tests.kt
package tests
import libPackage.Marker

// TESTCASE NUMBER: 1

class Case1() {
    fun Marker.foo(y: Int) = TODO()

    fun test() {
        Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
        Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>
    }
}

fun case1() {
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>
}

// TESTCASE NUMBER: 2
class Case2() {
    fun test() {
        fun Marker.foo(x:Int) = TODO()
        Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
        Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>
    }
}

fun Marker.foo(x:Int) = TODO()

fun case2() {
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>
}

// TESTCASE NUMBER: 3
fun case3() {
    fun Marker.foo(y:Int) = TODO()
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37391
 */
fun case4(marker : Marker?) {
    marker?.<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
    marker?.<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>><!SYNTAX!><!>
}


/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37391
 */
fun case5(marker : Marker?) {
    fun bar(){
        fun Marker.foo(y:Int) = TODO()
        marker?.<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
        marker?.<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>
    }
    marker?.<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo(y=1)<!>
    marker?.<!DEBUG_INFO_CALL("fqName: libPackage.Marker.invoke; typeCall: variable&invoke")!>foo(x=1)<!>
}
