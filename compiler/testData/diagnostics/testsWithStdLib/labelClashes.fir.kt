// LANGUAGE: -ContextReceivers
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-49015, KT-51433
// FIR_DUMP

fun Int.with() {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>("") <!CANNOT_INFER_PARAMETER_TYPE!>{
        this@with.<!UNRESOLVED_REFERENCE!>inc<!>()
    }<!>
}

fun Int.bar() {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>("") bar@<!CANNOT_INFER_PARAMETER_TYPE!>{
        this@bar.<!UNRESOLVED_REFERENCE!>inc<!>()
    }<!>
}

fun foo(f: with.() -> Unit) {}

class with {
    fun foo() {
        <!CANNOT_INFER_PARAMETER_TYPE!>with<!>("") <!CANNOT_INFER_PARAMETER_TYPE!>{
            this@with.<!UNRESOLVED_REFERENCE!>foo<!>()
        }<!>

        <!CANNOT_INFER_PARAMETER_TYPE!>with<!>("") with@<!CANNOT_INFER_PARAMETER_TYPE!>{
            this@with.<!UNRESOLVED_REFERENCE!>foo<!>()
        }<!>

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
