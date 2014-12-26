native
fun foo(a: String): Int = noImpl

native
fun Int.foo(a: String): Int = noImpl

native
class Bar(b: Int, c: Char) {
    fun baz(d: Int) {}
}

native
object Obj {
    fun test1(e: String) {}
    object Nested {
        fun test2(g: Int) {}
    }
}
