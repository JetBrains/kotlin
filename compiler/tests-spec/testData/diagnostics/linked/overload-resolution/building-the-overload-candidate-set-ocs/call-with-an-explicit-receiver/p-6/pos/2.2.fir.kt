// SKIP_TXT

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
            <!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!>      //resolves to (1) !!!
            this.<!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!> //resolves to (2)
        }
        "".apply {
            <!DEBUG_INFO_CALL("fqName: Case1.bar.foo; typeCall: extension function")!>foo()<!>      //resolves to (1) !!!
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
