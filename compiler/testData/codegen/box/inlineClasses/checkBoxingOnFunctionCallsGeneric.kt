// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineNotNullPrimitive<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineNotNullReference<T: String>(val y: T)

fun <A, T: Int> testNotNullPrimitive(a: Any, b: A, c: InlineNotNullPrimitive<T>, d: InlineNotNullPrimitive<T>?) {}
fun <A, T: String> testNotNullReference(a: Any, b: A, c: InlineNotNullReference<T>, d: InlineNotNullReference<T>?) {}

fun test(a: InlineNotNullPrimitive<Int>, b: InlineNotNullReference<String>) {
    testNotNullPrimitive(a, a, a, a) // 3 box
    testNotNullReference(b, b, b, b) // 2 box
}

fun box(): String {
    val a = InlineNotNullPrimitive(10)
    val b = InlineNotNullReference("some")

    test(a, b)

    return "OK"
}