// !LANGUAGE: +ContextReceivers

class Some {
    context(Some, String)
    fun foo() {
        <!NO_THIS!>this@foo<!>
        this<!LABEL_RESOLVE_WILL_CHANGE("class Some; function foo context receiver")!>@Some<!>
        this@String
    }

    context(Some)
    val self: Some
        get() = this<!LABEL_RESOLVE_WILL_CHANGE("class Some; property self context receiver")!>@Some<!>
}
