// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(private val data: T) {
    fun result(): String = if (data == 1) "OK" else "fail"
}

fun f(): UInt<Int> {
    val unull = UInt(1) ?: null
    return nonNull(unull)
}

fun <T: Int> nonNull(u: UInt<T>?) = u!!

fun box(): String {
    return f().result()
}