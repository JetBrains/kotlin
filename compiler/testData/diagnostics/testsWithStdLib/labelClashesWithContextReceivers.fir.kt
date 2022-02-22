// !LANGUAGE: +ContextReceivers

class Some {
    context(Some, String)
    fun foo() {
        this<!UNRESOLVED_LABEL!>@foo<!>
        this@Some
        this<!UNRESOLVED_LABEL!>@String<!>
    }

    context(Some)
    val self: Some
        get() = this@Some
}
