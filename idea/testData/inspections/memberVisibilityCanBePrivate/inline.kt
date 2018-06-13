open class AA {
    protected inline fun foo() {
        val result = bar()
    }
    protected fun bar() {
    }
}

fun <T> run(f: () -> T): T = f()

object TT {
    inline fun foo(f: () -> String) {
        run {
            bar(f())
        }
    }

    val x: String
        inline get() = baz

    fun bar(s: String) = s

    val baz = ""
}