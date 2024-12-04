// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: String>(val x: T) {
    private fun privateFun() = x
    override fun toString() = privateFun()
}

fun box(): String {
    val x: Any = IC("OK")
    return x.toString()
}