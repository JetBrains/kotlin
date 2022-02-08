// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Composed(val s: String) {

    constructor(s: String, x: Int) : this(s.subSequence(0, x).toString())

    private constructor(s1: String, s2: String) : this(s1 + s2, 2)

    fun p1(s2: String) =
        { Composed(s, s2) }
}

fun box() = Composed("O").p1("K1234")().s