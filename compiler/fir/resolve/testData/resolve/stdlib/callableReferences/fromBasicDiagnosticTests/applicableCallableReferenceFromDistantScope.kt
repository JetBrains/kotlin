fun foo(s: String) {}

object Scope {
    fun foo(a: Int) {}
    fun foo(b: Boolean) {}

    fun <T> bar(f: (T) -> Unit): T = TODO()

    fun test() {
        val s: String = bar(::foo)
    }
}
