// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 3
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 4
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 11 -> sentence 1
 * overload-resolution, c-level-partition -> paragraph 1 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: sets of non-extension member callables, local, explicitly imported, declared in the package scope and star-imported extension callables
 */

// TESTCASE NUMBER: 1, 2, 3

// FILE: Marker.kt
package libPackage

class Marker {
    fun foo() = println("non-extension member Marker.foo()")
    val foo: String = "non-extension member Marker.foo"
}

// FILE: Tests.kt
package tests
import libPackage.Marker

// TESTCASE NUMBER: 1
class Case1() {

    fun Marker.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() = println("local extension marker.foo")

    fun test() {
        Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo()<!>
    }
}

fun case1() {
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 2
class Case2() {
    fun test() {
        fun Marker.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() = println("local extension marker.foo")
        Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo()<!>
    }
}

fun Marker.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() = println("top level extension marker.foo")

fun case2() {
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 3
fun case3() {
    fun Marker.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() = println("local extension marker.foo")
    Marker().<!DEBUG_INFO_CALL("fqName: libPackage.Marker.foo; typeCall: function")!>foo()<!>
}
