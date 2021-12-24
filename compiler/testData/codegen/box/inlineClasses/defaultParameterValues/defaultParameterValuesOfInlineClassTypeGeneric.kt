// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val z: T)

fun test(z: Z<Int> = Z(42)) = z.z

fun box(): String {
    if (test() != 42) throw AssertionError()
    if (test(Z(123)) != 123) throw AssertionError()

    return "OK"
}