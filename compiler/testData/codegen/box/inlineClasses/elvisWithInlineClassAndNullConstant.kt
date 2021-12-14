// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt(private val data: Int) {
    fun result(): String = if (data == 1) "OK" else "fail"
}

fun f(): UInt {
    val unull = UInt(1) ?: null
    return nonNull(unull)
}

fun nonNull(u: UInt?) = u!!

fun box(): String {
    return f().result()
}