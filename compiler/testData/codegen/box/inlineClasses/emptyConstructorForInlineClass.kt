// WITH_RUNTIME

@JvmInline
value class Foo(val s: String) {
    fun asResult(): String = s
}

fun box(): String {
    val a = Foo("OK")
    return a.asResult()
}