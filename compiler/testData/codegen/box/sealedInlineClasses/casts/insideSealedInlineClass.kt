// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC {
    fun isO(): Boolean = this is O
    fun notIsO(): Boolean = this !is O
    fun asO(): O = this as O
    fun safeAsO(): O? = this as? O
}

value object O: IC()

value object O1: IC()

fun box(): String {
    val o = O
    if (!o.isO()) return "FAIL 1"
    if (o.notIsO()) return "FAIL 2"
    if (o.asO() != o) return "FAIL 3"
    if (o.safeAsO() != o) return "FAIL 4"
    return "OK"
}
