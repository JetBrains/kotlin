// !LANGUAGE: +ContextReceivers
// !RENDER_DIAGNOSTICS_FULL_TEXT

class Some {
    context(Some, String)
    fun foo() {
        //this@foo
        this@Some
        this@String
    }

    context(Some)
    val self: Some
        get() = this@Some
}
