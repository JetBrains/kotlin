// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T = 1234 as T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T = 1234L as T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T = "foobar" as T)

fun box(): String {
    if (Z<Int>().x != 1234) throw AssertionError()
    if (L<Long>().x != 1234L) throw AssertionError()
    if (S<String>().x != "foobar") throw AssertionError()

    return "OK"
}