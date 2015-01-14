nativeGetter
fun Int.foo(a: String): Int? = noImpl

native
class Bar(b: Int, c: Char) {
    nativeGetter
    fun baz(d: Int): Any? = noImpl
}

native
object Obj {
    nativeGetter
    fun test1(e: String): String? = noImpl

    object Nested {
        nativeGetter
        fun test2(g: Int): Any? = noImpl
    }
}
