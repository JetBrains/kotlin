fun bar(): Int = 0

object Scope {
    fun <T> foo(f: () -> T): T = f()

    fun bar(x: Int = 0): String = ""

    fun test() {
        val r1 = foo(::bar)
    }
}