// !DUMP_CFG
class InternalCalls {
    val s: String

    fun foo() {
        <!LEAKING_THIS!>s<!>.length
    }

    init {
        foo()
        s = ""
    }
}
