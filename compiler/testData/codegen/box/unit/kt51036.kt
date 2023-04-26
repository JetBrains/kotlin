// WITH_STDLIB

fun box(): String {
    A().close()
    return "OK"
}

class A {
    companion object;
    operator fun String.invoke() = Unit
    fun close() = kotlin.run { "abc" }()
}
