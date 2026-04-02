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
 * NUMBER: 2
 * DESCRIPTION: extension calls with explicit and implicit receiver
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36475
 */

// TESTCASE NUMBER: 1
class Case1 {
    fun bar() {
        val foo: String.() -> Unit = {} // (1)
        fun String.foo(): Unit {} // (2)
        "1".<!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!> // resolves to (2)
        with("2") {
            foo()      //resolves to (1) !!!
            this.<!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!> //resolves to (2)
        }
        "".run {
            <!DEBUG_INFO_CALL("fqName: kotlin.Function1.invoke; typeCall: variable&invoke")!>foo()<!>      //resolves to (1) !!!
            this.<!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!> //resolves to (2)
        }
        "".apply {
            <!DEBUG_INFO_CALL("fqName: kotlin.Function1.invoke; typeCall: variable&invoke")!>foo()<!>      //resolves to (1) !!!
            this.<!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!> //resolves to (2)
        }
        "".also {
            it.<!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!>   //resolves to (2)
        }
        "".let {
            it.<!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!>   //resolves to (2)
        }
    }
}

class B {
    val foo: String.() -> Unit = {} // (1)
    fun String.foo(): Unit {} // (2)
    fun bar() {
        "1".<!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!> // resolves to (2)
        val str = "1"
        with("2") {
            <!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!>      //resolves to (2)
            this.<!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!> //resolves to (2)
        }
        "".run {
            <!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!>      //resolves to (2)
            this.<!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!> //resolves to (2)
        }
        "".apply {
            <!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!>      //resolves to (2)
            this.<!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!> //resolves to (2)
        }
        "".also {
            it.<!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!>   //resolves to (2)
        }
        "".let {
            it.<!DEBUG_INFO_CALL("fqName: B.foo; typeCall: extension function")!>foo()<!>   //resolves to (2)
        }
    }
}
