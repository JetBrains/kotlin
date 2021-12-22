// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Int>(private val r: T) {
    fun test() =
        object {
            override fun toString() = "OK"
        }.toString()
}

fun box() = R(0).test()