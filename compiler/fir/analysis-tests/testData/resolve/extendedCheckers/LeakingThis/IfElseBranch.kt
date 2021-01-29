// !DUMP_CFG

class B(v: Boolean) {
    val a: String

    fun foo() {
        a.length
    }

    init {
        if (v) {
            a = "123"
        }

        foo()
    }
}
