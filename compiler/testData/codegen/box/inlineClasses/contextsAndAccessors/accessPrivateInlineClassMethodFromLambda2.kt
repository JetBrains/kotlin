// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class R(private val r: Long) {
    fun test() = { ok() }.let { it() }

    private fun ok() = "OK"
}

fun box() = R(0).test()