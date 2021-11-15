// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo<T>(val x: Any) {
    fun bar() {}
}

fun <T, K> transform(f: Foo<T>): Foo<K> {
    return when {
        true -> f as Foo<K>
        else -> TODO()
    }
}

fun box(): String {
    val f = Foo<Int>(42)
    val t = transform<Int, Number>(f)
    return if (t.x !is Number) "Fail" else "OK"
}
