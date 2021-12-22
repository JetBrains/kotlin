// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Int>(private val r: T) {
    private fun ok() = "OK"

    companion object {
        fun test(r: R<Int>) = r.ok()
    }
}

fun box() = R.test(R(0))