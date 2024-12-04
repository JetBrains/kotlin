// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Composed<T: String>(val s: T) {

    constructor(s: String, x: Int) : this(s.subSequence(0, x).toString() as T)

    private constructor(s1: String, s2: String) : this((s1 + s2) as T, 2)

    fun p1(s2: String) =
        { Composed<String>(s, s2) }
}

fun box() = Composed<String>("O").p1("K1234")().s