// JVM backend throws runtime exception: java.lang.ClassCastException: kotlin.Unit cannot be cast to Foo
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter, +ValueClassesSecondaryConstructorWithBody

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: String>(val x: T) {
    constructor(y: Int) : this("OK" as T) {
        if (y == 0) throw IllegalArgumentException()
        if (y == 1) return
        return Unit
    }

    constructor(z: Double) : this(z.toInt())
}

fun box(): String {
    return Foo<String>(42.0).x
}
