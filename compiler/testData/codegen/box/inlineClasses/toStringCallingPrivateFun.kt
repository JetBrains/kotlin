// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val x: String) {
    private fun privateFun() = x
    override fun toString() = privateFun()
}

fun box(): String {
    val x: Any = IC("OK")
    return x.toString()
}