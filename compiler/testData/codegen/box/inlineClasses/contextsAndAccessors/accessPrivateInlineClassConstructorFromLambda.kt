// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Composed(val s: String) {

    constructor(s: String, x: Int) : this(s.subSequence(0, x).toString())

    private constructor(s1: String, s2: String) : this(s1 + s2, 2)

    fun p1(s2: String) =
        { Composed(s, s2) }
}

fun box() = Composed("O").p1("K1234")().s