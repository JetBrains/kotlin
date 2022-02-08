// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface Base {
    fun result(): Int
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inlined(val x: Int) : Base {
    override fun result(): Int = x
}

fun foo(b: Boolean): Base {
    return if (b) Inlined(0) else Inlined(1)
}

fun box(): String {
    if (foo(true).result() != 0) return "Fail 1"
    if (foo(false).result() != 1) return "Fail 2"
    return "OK"
}