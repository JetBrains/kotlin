// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -EXTENSION_SHADOWED_BY_MEMBER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 11 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: The sets of non-extension member callables named f of type T;
 */

class Marker {
    fun foo() = println("non-extension member toplevel Marker.foo()")
    val foo: String = "non-extension member toplevel Marker.foo"
}

// TESTCASE NUMBER: 1
class Case1() {

    fun Marker.foo() = println("local extension marker.foo")

    fun test() {
        Marker().<!DEBUG_INFO_CALL("fqName: Marker.foo; typeCall: function")!>foo()<!>
    }
}

fun case1() {
    Marker().<!DEBUG_INFO_CALL("fqName: Marker.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 2
class Case2() {
    fun test() {
        fun Marker.foo() = println("local extension marker.foo")
        Marker().<!DEBUG_INFO_CALL("fqName: Marker.foo; typeCall: function")!>foo()<!>
    }
}

fun Marker.foo() = println("top level extension marker.foo")

fun case2() {
    Marker().<!DEBUG_INFO_CALL("fqName: Marker.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 3
fun case3() {
    fun Marker.foo() = println("local extension marker.foo")
    Marker().<!DEBUG_INFO_CALL("fqName: Marker.foo; typeCall: function")!>foo()<!>
}
