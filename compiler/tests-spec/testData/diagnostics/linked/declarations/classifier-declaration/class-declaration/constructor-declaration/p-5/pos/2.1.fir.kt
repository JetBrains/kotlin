// !DIAGNOSTICS: -UNUSED_PARAMETER -UNRESOLVED_REFERENCE -UNREACHABLE_CODE
// SKIP_TXT


// TESTCASE NUMBER: 1
class A(val x: Any?) {
    init {
        x checkType { check<Any?>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    }

    fun test() {
        x checkType { check<Any?>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    }
}

class B(val x: Any) {
    init {
        x checkType { check<Any>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
    }

    fun test() {
        x checkType { check<Any>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
    }
}

class C(val x: () -> Any) {
    init {
        x checkType { check<() -> Any>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Any>")!>x<!>
    }

    fun test() {
        x checkType { check<() -> Any>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Any>")!>x<!>
    }
}

class D(val x: Enum<*>) {
    init {
        x checkType { check<Enum<*>>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x<!>
    }

    fun test() {
        x checkType { check<Enum<*>>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x<!>
    }
}

class E(val x: Nothing) {
    init {
        x checkType { check<Nothing>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
    }

    fun test() {
        x checkType { check<Nothing>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
    }
}

class F<T>(val x: T) {
    init {
        x checkType { check<T>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
    }

    fun test() {
        x checkType { check<T>() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
    }
}
