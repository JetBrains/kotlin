// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

class Test(val s: S) {
    constructor(x: String, s: S) : this(S(x + s.string))
}

fun box() = Test("O", S("K")).s.string