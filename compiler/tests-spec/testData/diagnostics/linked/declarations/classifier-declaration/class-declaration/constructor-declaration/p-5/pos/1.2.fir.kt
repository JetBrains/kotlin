// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1 {
    class A(x: Any?){
        init {
            x checkType { check<Any?>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        }
    }
    class B(x: Any){
        init {
            x checkType { check<Any>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        }
    }
    class C(x: () -> Any){
        init {
            x checkType { check<()->Any>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Any>")!>x<!>
        }
    }
    class D(x: Enum<*>){
        init {
            x checkType { check<Enum<*>>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x<!>
        }
    }
    class E(x: Nothing){
        init {
            x checkType { check<Nothing>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        }
    }
    class F<T>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
}

// TESTCASE NUMBER: 2
class Case2<T>() {
    class A<T : CharSequence>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
    class B<T : java.util.AbstractCollection<Int>>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
    class C<T : java.lang.Exception>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
    class D<T : Enum<*>>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
    class F<T>(x: T){
        init {
            x checkType { check<T>() }
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        }
    }
}
