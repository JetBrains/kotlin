// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val s: String) {
    val Int.s: Int get() = 42
}

fun box(): String {
    if (Z("a").toString() == "Z(s=a)")
        return "OK"
    return "Fail"
}
