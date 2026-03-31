// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
abstract class X(val y: Bar)

object Bar {
    <!UNINITIALIZED_PROPERTY!>val prop = <!UNINITIALIZED_ACCESS!>Foo.const<!><!>
}

class Foo {
    companion object : X(Bar) {
        val const = "AAA"
    }
}
