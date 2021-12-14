// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val z: Int)

fun test(z: Z = Z(42)) = z.z

fun box(): String {
    if (test() != 42) throw AssertionError()
    if (test(Z(123)) != 123) throw AssertionError()

    return "OK"
}