class A<T, U : Any> {
    fun foo() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>T::<!UNRESOLVED_REFERENCE!>toString<!><!>

    fun bar() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>U::<!UNRESOLVED_REFERENCE!>toString<!><!>
}

fun <T> foo() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>T::<!UNRESOLVED_REFERENCE!>toString<!><!>

fun <U : Any> bar() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>U::<!UNRESOLVED_REFERENCE!>toString<!><!>
