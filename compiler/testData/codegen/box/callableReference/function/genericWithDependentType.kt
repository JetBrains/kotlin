// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

class Wrapper<T>(val value: T)

fun box(): String {
    val ls = listOf("OK").map(::Wrapper)
    return ls[0].value
}
