// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * PRIMARY LINKS: overload-resolution, c-level-partition -> paragraph 1 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 3 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 2 -> sentence 1
 * overload-resolution, receivers -> paragraph 7 -> sentence 2
 * overload-resolution, receivers -> paragraph 7 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: set of non-extension member callables only
 */

// TESTCASE NUMBER: 1
interface Marker {
    operator fun invoke() {}
}

class Case1() {
    fun foo() : Int =1

    val foo = object : Marker {}

    fun innerFun() {
        <!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo()<!>
        this.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo()<!>
    }

    inner class InnerClass0 {
        val foo = object : Marker {}

        fun innerClassFun() {
            <!DEBUG_INFO_CALL("fqName: Marker.invoke; typeCall: variable&invoke")!>foo()<!>
            this.<!DEBUG_INFO_CALL("fqName: Marker.invoke; typeCall: variable&invoke")!>foo()<!>
            this@Case1.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo()<!>
        }
    }
    inner class InnerClass1 {
        fun foo() : Int =1

        fun innerClassFun() {
            <!DEBUG_INFO_CALL("fqName: Case1.InnerClass1.foo; typeCall: function")!>foo()<!>
            this.<!DEBUG_INFO_CALL("fqName: Case1.InnerClass1.foo; typeCall: function")!>foo()<!>
            this@Case1.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo()<!>
        }
    }
}

