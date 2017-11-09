// "Surround with null check" "true"

fun foo(p: String?) {
    Util.f1(Util.f2(p<caret>.length), 0)
}

object Util {
    fun f1(o: Any, p: Int): Any = o
    fun f2(o: Any): Any = o
}