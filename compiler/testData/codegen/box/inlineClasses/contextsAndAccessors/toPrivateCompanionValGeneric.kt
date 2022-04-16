// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Int>(private val r: T) {
    fun test() = pv

    companion object {
        private val pv = "OK"
    }
}

fun box() = R(0).test()