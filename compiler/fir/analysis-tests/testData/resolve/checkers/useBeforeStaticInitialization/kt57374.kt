// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

sealed class Base {
    companion object {
        <!UNINITIALIZED_PROPERTY!>val fooAccess = <!UNINITIALIZED_ACCESS!>Derived.foo()<!><!>
    }
}

class Derived(var value: String) : Base() {
    companion object {
        fun foo(): String = "foo"
    }
}
