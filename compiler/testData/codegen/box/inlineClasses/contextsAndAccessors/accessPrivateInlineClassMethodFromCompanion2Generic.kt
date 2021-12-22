// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Long>(private val r: T) {
    private fun ok() = "OK"

    companion object {
        fun test(r: R<Long>) = r.ok()
    }
}

fun box() = R.test(R(0))