// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Any>(val x: T) {
    fun bar() {}
}

fun <T: Any, K: Any> transform(f: Foo<T>): Foo<K> {
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
