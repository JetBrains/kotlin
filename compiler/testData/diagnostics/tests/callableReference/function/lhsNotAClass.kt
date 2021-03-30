// !WITH_NEW_INFERENCE

class A<T, U : Any> {
    fun foo() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>T::<!UNRESOLVED_REFERENCE!>toString<!><!>

    fun bar() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>U::<!UNRESOLVED_REFERENCE!>toString<!><!>

    fun baz() {
        take(<!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>T::<!UNRESOLVED_REFERENCE{OI}!>toString<!><!>)

        take(<!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>U::<!UNRESOLVED_REFERENCE{OI}!>toString<!><!>)
    }
}

fun <T> foo() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>T::<!UNRESOLVED_REFERENCE!>toString<!><!>

fun <U : Any> bar() = <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>U::<!UNRESOLVED_REFERENCE!>toString<!><!>

fun take(arg: Any) {}
