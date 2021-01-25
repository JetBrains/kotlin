// !DUMP_CFG

class MoreThanOneInit {
    val s: String
    val t: String

    fun foo() {
        t.length
        s.length
    }

    init {
        s = "test"
    }

    init {
        t = "test"
        foo()
    }
}
