// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Composed(val s: String) {
    private constructor(s1: String, s2: String) : this(s1 + s2)

    companion object {
        fun p1(s: String) = Composed("O", s)
    }
}

fun box() = Composed.p1("K").s