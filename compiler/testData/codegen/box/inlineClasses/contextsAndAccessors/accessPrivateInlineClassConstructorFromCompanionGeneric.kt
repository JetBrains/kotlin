// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Composed<T: String>(val s: T) {
    private constructor(s1: String, s2: String) : this((s1 + s2) as T)

    companion object {
        fun p1(s: String) = Composed<String>("O", s)
    }
}

fun box() = Composed.p1("K").s