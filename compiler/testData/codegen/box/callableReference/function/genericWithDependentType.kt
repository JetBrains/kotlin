// WITH_STDLIB

class Wrapper<T>(val value: T)

fun box(): String {
    val ls = listOf("OK").map(::Wrapper)
    return ls[0].value
}
