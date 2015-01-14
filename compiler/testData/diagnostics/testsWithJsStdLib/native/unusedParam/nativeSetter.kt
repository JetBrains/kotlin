nativeSetter
fun Int.foo(a: String, v: Int): Int = noImpl

native
class Bar(b: Int, c: Char) {
    nativeSetter
    fun baz(d: Int, v: Int) {}
}

native
object Obj {
    nativeSetter
    fun test1(e: String, v: Any) {}

    object Nested {
        nativeSetter
        fun test2(g: Int, v: Any) {}
    }
}
