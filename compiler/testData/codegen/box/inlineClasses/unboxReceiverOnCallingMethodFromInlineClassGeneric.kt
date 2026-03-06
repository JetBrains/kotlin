// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Any>(val s: T) {
    fun isString(): Boolean = s is String
}

class Box<T>(val x: T)

fun box(): String {
    val f = Foo("string")
    val g = Box(f)
    val r = g.x.isString()

    if (!r) return "Fail"

    return "OK"
}