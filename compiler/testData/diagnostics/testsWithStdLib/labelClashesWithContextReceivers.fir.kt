// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-49015, KT-51433
// FIR_DUMP

class Some {
    context(Some, String)
    fun foo() {
        //this@foo
        this<!AMBIGUOUS_LABEL!>@Some<!>
        this@String
    }

    context(Some)
    val self: Some
        get() = this<!AMBIGUOUS_LABEL!>@Some<!>
}

private typealias Extension = TypedThis

class TypedThis {
    fun TypedThis.baz() {
        this<!AMBIGUOUS_LABEL!>@TypedThis<!>
    }

    fun Extension.bar() {
        this@TypedThis
    }
}
