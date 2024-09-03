fun interface SamInterface {
    fun accept(a: Int, i: String): String

}
fun Int.foo(i: String): String = i
val a = SamInterface (Int::foo)

fun box(): String {
    with(a) {
        return accept(1, "OK")
    }
}