// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextReceivers
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-49015, KT-51433
// FIR_DUMP

fun Int.with() {
    with("") {
        this<!LABEL_RESOLVE_WILL_CHANGE("function with; anonymous function")!>@with<!>.inc()
    }
}

fun Int.bar() {
    with("") bar@{
        this<!LABEL_RESOLVE_WILL_CHANGE!>@bar<!>.inc()
    }
}

fun foo(f: with.() -> Unit) {}

class with {
    fun foo() {
        with("") {
            this<!LABEL_RESOLVE_WILL_CHANGE("class with; anonymous function")!>@with<!>.foo()
        }

        with("") with@{
            this<!LABEL_RESOLVE_WILL_CHANGE!>@with<!>.foo()
        }

        with("") other@{
            this@with.foo()
        }
    }
}

private typealias Extension = TypedThis

class TypedThis {
    fun TypedThis.baz() {
        this<!LABEL_RESOLVE_WILL_CHANGE("class TypedThis; function baz extension receiver")!>@TypedThis<!>
    }

    fun Extension.bar() {
        this@TypedThis
    }
}

object OtherTests {
    fun Int.with() {
        with("") {
            this<!LABEL_RESOLVE_WILL_CHANGE!>@with<!>.toString()
            this<!LABEL_RESOLVE_WILL_CHANGE!>@with<!>.<!UNRESOLVED_REFERENCE!>length<!>
            this<!LABEL_RESOLVE_WILL_CHANGE!>@with<!>.inc()
        }
    }
}

object OtherTests2 {
    fun Int.with() {
        with("") {
            this<!LABEL_RESOLVE_WILL_CHANGE!>@with<!>.toString()
        }
    }
}
