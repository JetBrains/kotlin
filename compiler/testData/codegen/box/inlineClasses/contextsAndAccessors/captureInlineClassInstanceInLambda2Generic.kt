// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Long>(private val r: T) {
    fun test() = { ok() }.let { it() }

    fun ok() = "OK"
}

fun box() = R(0).test()