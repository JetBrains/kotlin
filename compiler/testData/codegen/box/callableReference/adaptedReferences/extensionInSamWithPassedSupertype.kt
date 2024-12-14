var result = ""

fun interface SamInterface {
    fun Int.accept(i: String): String
}

fun Number.foo(i: String): String = i
fun bar(a: Number, i: String): String = i

val a = SamInterface(Number::foo)
val b = SamInterface(::bar)

fun box(): String {
    with(a) {
        result += 1.accept("O")
    }
    with(b) {
        result += 1.accept("K")
    }
    return result
}