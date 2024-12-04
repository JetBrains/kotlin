// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 4
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 9 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: sets of extension callables declared in the package scope
 */

package tests.diag

// TESTCASE NUMBER: 1, 2
operator fun Int.invoke() {}

// TESTCASE NUMBER: 3, 4
interface Marker {
    operator fun invoke() {}
}

// TESTCASE NUMBER: 1
class Case1 {
    val foo: Int = 1
}

fun case1() {
    Case1().<!DEBUG_INFO_CALL("fqName: tests.diag.invoke; typeCall: variable&invoke")!>foo()<!>
    val a = Case1()
    a.<!DEBUG_INFO_CALL("fqName: tests.diag.invoke; typeCall: variable&invoke")!>foo()<!>
    var b = Case1()
    b.<!DEBUG_INFO_CALL("fqName: tests.diag.invoke; typeCall: variable&invoke")!>foo()<!>
}

// TESTCASE NUMBER: 2
class Case2 {
    val foo: Int = 1
    fun foo(): Int = 1
}

fun case2() {
    Case2().<!DEBUG_INFO_CALL("fqName: tests.diag.Case2.foo; typeCall: function")!>foo()<!>
    val a = Case2()
    a.<!DEBUG_INFO_CALL("fqName: tests.diag.Case2.foo; typeCall: function")!>foo()<!>
    var b = Case2()
    b.<!DEBUG_INFO_CALL("fqName: tests.diag.Case2.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 3
class Case3() {
    val foo = object : Marker {}
    fun foo(): Int = 1

    fun f() {
        this.<!DEBUG_INFO_CALL("fqName: tests.diag.Case3.foo; typeCall: function")!>foo()<!>
    }
}

fun case3() {
    Case3().<!DEBUG_INFO_CALL("fqName: tests.diag.Case3.foo; typeCall: function")!>foo()<!>
    val a = Case3()
    a.<!DEBUG_INFO_CALL("fqName: tests.diag.Case3.foo; typeCall: function")!>foo()<!>
    var b = Case3()
    b.<!DEBUG_INFO_CALL("fqName: tests.diag.Case3.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 4
class Case4() {
    val foo = object : Marker {}

    fun f() {
       this.<!DEBUG_INFO_CALL("fqName: tests.diag.Marker.invoke; typeCall: variable&invoke")!>foo()<!>
    }
}

fun case4() {
    Case4().<!DEBUG_INFO_CALL("fqName: tests.diag.Marker.invoke; typeCall: variable&invoke")!>foo()<!>
    val a = Case4()
    a.<!DEBUG_INFO_CALL("fqName: tests.diag.Marker.invoke; typeCall: variable&invoke")!>foo()<!>
    var b = Case4()
    b.<!DEBUG_INFO_CALL("fqName: tests.diag.Marker.invoke; typeCall: variable&invoke")!>foo()<!>
}


// TESTCASE NUMBER: 5
class Case5() {
    companion object foo {
        operator fun invoke() {}
    }

    fun innerFun() = this.<!DEBUG_INFO_CALL("fqName: tests.diag.Case5.foo; typeCall: function")!>foo()<!>

    fun foo(): Int  = 1

}

fun case5() {
    Case5().<!DEBUG_INFO_CALL("fqName: tests.diag.Case5.foo; typeCall: function")!>foo()<!>
    val a = Case5()
    a.<!DEBUG_INFO_CALL("fqName: tests.diag.Case5.foo; typeCall: function")!>foo()<!>

    var b = Case5()
    b.<!DEBUG_INFO_CALL("fqName: tests.diag.Case5.foo; typeCall: function")!>foo()<!>
}


// TESTCASE NUMBER: 6
class Case6() {
    companion object foo {
        operator fun invoke() {}
    }

    fun innerFun() = <!DEBUG_INFO_CALL("fqName: tests.diag.Case6.foo.invoke; typeCall: variable&invoke")!>foo()<!>
}

fun case6() {
    Case6.<!DEBUG_INFO_CALL("fqName: tests.diag.Case6.foo.invoke; typeCall: variable&invoke")!>foo()<!>
}

