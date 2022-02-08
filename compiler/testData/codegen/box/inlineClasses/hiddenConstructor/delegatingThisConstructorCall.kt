// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

class Test(val x: S, val y: S) {
    constructor(x: S) : this(x, S("K"))

    val test = x.string + y.string
}

fun box() = Test(S("O")).test