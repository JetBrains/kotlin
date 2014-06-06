val four: Int by NumberDecrypter

class A {
    val two: Int by NumberDecrypter
}

object NumberDecrypter {
    fun get(instance: Any?, data: PropertyMetadata) = when (data.name) {
        "four" -> 4
        "two" -> 2
        else -> throw AssertionError()
    }
}

fun box(): String {
    val x = ::four.get()
    if (x != 4) return "Fail x: $x"
    val a = A()
    val y = A::two.get(a)
    if (y != 2) return "Fail y: $y"
    return "OK"
}
