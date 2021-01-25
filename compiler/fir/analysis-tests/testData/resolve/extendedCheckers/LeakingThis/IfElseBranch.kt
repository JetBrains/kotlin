// !DUMP_CFG

class B(v: Boolean) {
    val a: String

    fun foo() {
        <!MAY_BE_NOT_INITIALIZED!>a<!>.length
    }

    init {
        if (v) {
            a = "123"
        }

        <!LEAKING_THIS!>foo()<!>
    }
}
