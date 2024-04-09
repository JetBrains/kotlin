// !LANGUAGE: +ContextReceivers
// !RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-49015, KT-51433
// FIR_DUMP

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

private typealias Extension = TypedThis

class TypedThis {
    fun TypedThis.baz() {
        this@TypedThis
    }

    fun Extension.bar() {
        this@TypedThis
    }
}
