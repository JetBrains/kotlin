// !RENDER_DIAGNOSTICS_FULL_TEXT

fun Int.with() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>("") {
        this@with.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun Int.bar() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>("") bar@{
        this@bar.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun foo(f: with.() -> Unit) {}

class with {
    fun foo() {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>("") {
            this@with.<!UNRESOLVED_REFERENCE!>foo<!>()
        }

        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>("") with@{
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
