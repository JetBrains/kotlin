// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 2
 * PRIMARY LINKS: overload-resolution, c-level-partition -> paragraph 1 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 3 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 3 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 2 -> sentence 1
 * overload-resolution, callables-and-invoke-convention -> paragraph 7 -> sentence 1
 * overload-resolution, callables-and-invoke-convention -> paragraph 7 -> sentence 2
 * overload-resolution, receivers -> paragraph 7 -> sentence 2
 * overload-resolution, receivers -> paragraph 7 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: The sets of local extension callables
 */

// TESTCASE NUMBER: 1
class Case1() {
    fun foo() : Int =1

    val foo = object : MarkerCase1 {}

    fun innerFun() {
        this.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo()<!>
    }


    inner class InnerClass0 {
        val foo = object : MarkerCase1 {}
        operator fun MarkerCase1.invoke() {}

        fun innerClassFun() {
            this.<!DEBUG_INFO_CALL("fqName: Case1.InnerClass0.invoke; typeCall: variable&invoke")!>foo()<!>
        }
    }

    operator fun MarkerCase1.invoke() {}

    inner class InnerClass1(){
        val foo = object : MarkerCase1 {}

        fun nestedClassFun(){
            this.<!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: variable&invoke")!>foo()<!>
        }
    }
}

interface MarkerCase1 {}

fun case1(){
    operator fun MarkerCase1.invoke() {}
    Case1().InnerClass0().<!DEBUG_INFO_CALL("fqName: case1.invoke; typeCall: variable&invoke")!>foo()<!>
    Case1().InnerClass1().<!DEBUG_INFO_CALL("fqName: case1.invoke; typeCall: variable&invoke")!>foo()<!>
}

// TESTCASE NUMBER: 2
open class Case2() {
    open fun fooCase2(): MarkerCase2 = object : MarkerCase2 {}

    open val fooCase2 = object : MarkerCase2 {}

    interface MarkerCase2 {}
    open operator fun MarkerCase2.invoke() {}

    fun innerFun() {
        <!DEBUG_INFO_CALL("fqName: Case2.fooCase2; typeCall: function")!>fooCase2()<!>
        this.<!DEBUG_INFO_CALL("fqName: Case2.fooCase2; typeCall: function")!>fooCase2()<!>
    }

    inner class InnerClass0 : Case2() {
        override fun fooCase2(): MarkerCase2 {
            return object : MarkerCase2 {}
        }

        override operator fun MarkerCase2.invoke() {}

        fun innerClassFun() {
            this@Case2.<!DEBUG_INFO_CALL("fqName: Case2.fooCase2; typeCall: function")!>fooCase2()<!>
            this.<!DEBUG_INFO_CALL("fqName: Case2.InnerClass0.fooCase2; typeCall: function")!>fooCase2()<!>
        }
    }


    inner class InnerClass1() {
        val fooCase2 = object : MarkerCase2 {}

        fun nestedClassFun() {
            this.<!DEBUG_INFO_CALL("fqName: Case2.invoke; typeCall: variable&invoke")!>fooCase2()<!>
        }
    }
}


fun case2() {
    operator fun Case2.MarkerCase2.invoke() {}
    Case2().InnerClass0().<!DEBUG_INFO_CALL("fqName: Case2.InnerClass0.fooCase2; typeCall: function")!>fooCase2()<!>
    Case2().InnerClass1().<!DEBUG_INFO_CALL("fqName: case2.invoke; typeCall: variable&invoke")!>fooCase2()<!>
}
