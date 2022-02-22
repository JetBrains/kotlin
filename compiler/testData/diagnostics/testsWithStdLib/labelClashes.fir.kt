fun Int.with() {
    with("") {
        <!ARGUMENT_TYPE_MISMATCH!>this@with.<!UNRESOLVED_REFERENCE!>inc<!>()<!>
    }
}

fun Int.bar() {
    with("") bar@{
        <!ARGUMENT_TYPE_MISMATCH!>this@bar.<!UNRESOLVED_REFERENCE!>inc<!>()<!>
    }
}

fun foo(f: with.() -> Unit) {}

class with {
    fun foo() {
        with("") {
            <!ARGUMENT_TYPE_MISMATCH!>this@with.<!UNRESOLVED_REFERENCE!>foo<!>()<!>
        }

        with("") with@{
            <!ARGUMENT_TYPE_MISMATCH!>this@with.<!UNRESOLVED_REFERENCE!>foo<!>()<!>
        }

        with("") other@{
            this@with.foo()
        }
    }
}

private typealias Extension = TypedThis

class TypedThis {
    fun TypedThis.baz() {
        this@TypedThis
    }

    fun Extension.bar() {
        this@TypedThis
    }
}
