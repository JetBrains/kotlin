// !LANGUAGE: +ContextReceivers
// !RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-49015, KT-51433
// FIR_DUMP

class Some {
    context(Some, String)
    fun foo() {
        //this@foo
        this<!LABEL_RESOLVE_WILL_CHANGE("class Some; function foo context receiver")!>@Some<!>
        this@String
    }

    context(Some)
    val self: Some
        get() = this<!LABEL_RESOLVE_WILL_CHANGE("class Some; property self context receiver")!>@Some<!>
}

private typealias Extension = TypedThis

class TypedThis {
    fun TypedThis.baz() {
        this<!LABEL_RESOLVE_WILL_CHANGE("class TypedThis; function baz context receiver")!>@TypedThis<!>
    }

    fun Extension.bar() {
        this@TypedThis
    }
}
