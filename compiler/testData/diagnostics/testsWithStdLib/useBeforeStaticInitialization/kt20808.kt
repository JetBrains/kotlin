// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
abstract class X(val y: Bar)

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object Bar<!> {
    <!POTENTIALLY_UNINITIALIZED_PROPERTY!>val prop = <!POTENTIALLY_UNINITIALIZED_ACCESS!>Foo.const<!><!>
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class Foo {
    companion object : X(Bar) {
        val const = "AAA"
    }
}<!>
