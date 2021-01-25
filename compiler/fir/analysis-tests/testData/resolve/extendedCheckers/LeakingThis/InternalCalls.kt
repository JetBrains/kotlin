// !DUMP_CFG
class InternalCalls {
    val s: String

    fun foo() {
        <!MAY_BE_NOT_INITIALIZED!>s<!>.length
    }

    init {
        <!LEAKING_THIS!>foo()<!>
        s = ""
    }
}
