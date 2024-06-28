// JVM runtime exception: java.lang.ClassCastException: kotlin.Unit cannot be cast to Foo
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +ValueClassesSecondaryConstructorWithBody

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo(val x: String) {
    constructor(y: Int) : this("OK") {
        if (y == 0) throw IllegalArgumentException()
        if (y == 1) return
        return Unit
    }

    constructor(z: Double) : this(z.toInt())
}

fun box(): String {
    return Foo(42.0).x
}
