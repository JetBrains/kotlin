// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class WithPrimitive<T: Int>(val a: T)
fun <T: Int> takeWithPrimitive(a: WithPrimitive<T>) {}

OPTIONAL_JVM_INLINE_ANNOTATION
value class WithReference<T: Any>(val a: T)
fun <T: Any> takeWithReference(a: WithReference<T>) {}

OPTIONAL_JVM_INLINE_ANNOTATION
value class WithNullableReference<T>(val a: T)
fun <T> takeWithNullableReference(a: WithNullableReference<T>) {}

OPTIONAL_JVM_INLINE_ANNOTATION
value class WithNullableReference2<T: Any>(val a: T?)
fun <T: Any> takeWithNullableReference2(a: WithNullableReference2<T>) {}

fun <T: Int> foo(a: WithPrimitive<T>?, b: WithPrimitive<T>) {
    takeWithPrimitive(a!!) // unbox
    takeWithPrimitive(a) // unbox
    takeWithPrimitive(b!!)
}

fun <T: Any, T2: Any> bar(a: WithReference<T>?, b: WithReference<T2>) {
    takeWithReference(a!!)
    takeWithReference(a)
    takeWithReference(b!!)
}

fun <T, R> baz(a: WithNullableReference<T>?, b: WithNullableReference<R>) {
    takeWithNullableReference(a!!) // unbox
    takeWithNullableReference(a) // unbox
    takeWithNullableReference(a!!) // unbox
    takeWithNullableReference(b!!)
}

fun <T: Any, R: Any> baz2(a: WithNullableReference2<T>?, b: WithNullableReference2<R>) {
    takeWithNullableReference2(a!!) // unbox
    takeWithNullableReference2(a) // unbox
    takeWithNullableReference2(a!!) // unbox
    takeWithNullableReference2(b!!)
}

fun box(): String {
    val a1 = WithPrimitive(1)
    val b1 = WithPrimitive(2)

    foo(a1, b1)

    val a2 = WithReference("")

    bar(a2, a2)

    val a3 = WithNullableReference("test")
    val a4 = WithNullableReference(123)

    baz(a3, a4)

    val a32 = WithNullableReference2("test")
    val a42 = WithNullableReference2(123)

    baz2(a32, a42)

    return "OK"
}