// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1 {
    inner class A(x: Any?){
        init {
            x checkType { check<Any?>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        }
    }

    inner class B(x: Any){
        init {
            x checkType { check<Any>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        }
    }

    inner class C(x: () -> Any){
        init {
            x checkType { check<()->Any>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Any>")!>x<!>
        }
    }

    inner class D(x: Enum<*>){
        init {
            x checkType { check<Enum<*>>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x<!>
        }
    }

    inner class E(x: Nothing){
        init {
            x checkType { check<Nothing>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        }
    }

    inner class F<T>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
}

// TESTCASE NUMBER: 2
class Case2<T>() {
    inner class A(x: Any?, t: T){
        init {
            t checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>t<!>

            x checkType { check<Any?>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        }
    }

    inner class B(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }

    inner class C(x: () -> T){
        init {
            x checkType { check<() -> T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<T>")!>x<!>
        }
    }

    inner class D<T : Enum<*>>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }

    inner class E(n: Nothing, x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>

            n checkType { check<Nothing>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>n<!>
        }
    }

    inner class F<T>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
}
