// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

class Outer(val s1: S<String>) {
    inner class Inner(val s2: S<String>) {
        val test = s1.string + s2.string
    }
}

fun box() = Outer(S("O")).Inner(S("K")).test