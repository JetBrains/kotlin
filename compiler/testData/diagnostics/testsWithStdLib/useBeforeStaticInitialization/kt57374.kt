// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class Base {
    companion object {
        <!POTENTIALLY_UNINITIALIZED_PROPERTY!>val fooAccess = <!POTENTIALLY_UNINITIALIZED_ACCESS!>Derived.foo()<!><!>
    }
}<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class Derived(var value: String) : Base() {
    companion object {
        fun foo(): String = "foo"
    }
}<!>
