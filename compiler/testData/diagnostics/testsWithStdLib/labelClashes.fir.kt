// !RENDER_DIAGNOSTICS_FULL_TEXT

fun Int.with() {
    with("") {
        this@with.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun Int.bar() {
    with("") bar@{
        this@bar.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun foo(f: with.() -> Unit) {}

class with {
    fun foo() {
        with("") {
            this@with.<!UNRESOLVED_REFERENCE!>foo<!>()
        }

        with("") with@{
            this@with.<!UNRESOLVED_REFERENCE!>foo<!>()
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
