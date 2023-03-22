// IGNORE_REVERSED_RESOLVE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class A(x: Any?){
    init {
        x checkType { check<Any?>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    }
}
// TESTCASE NUMBER: 2
class B(x: Any){
    init {
        x checkType { check<Any>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
    }
}
// TESTCASE NUMBER: 3
class C(x: () -> Any){
    init {
        x checkType { check<()->Any>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Any>")!>x<!>
    }
}
// TESTCASE NUMBER: 4
class D(x: Enum<*>){
    init {
        x checkType { check<Enum<*>>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x<!>
    }
}
// TESTCASE NUMBER: 5
class E(x: Nothing){
    init {
        x checkType { check<Nothing>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
    }
}
// TESTCASE NUMBER: 6
class F<T>(x: T){
    init {
        x checkType { check<T>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
    }
}
