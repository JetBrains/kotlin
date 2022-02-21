// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineNotNullPrimitive<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineNullablePrimitive<T: Int?>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineNotNullReference<T: Any>(val a: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineNullableReference<T>(val a: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineNullableReference2<T: Any>(val a: T?)

fun <T: Int> test1(a: InlineNotNullPrimitive<T>) {
    val a0 = a
    val a1: Any = a // box
    val a2: Any? = a // box
    val a3: InlineNotNullPrimitive<T> = a
    val a4: InlineNotNullPrimitive<T>? = a // box
}

fun <T: Int?> test2(b: InlineNullablePrimitive<T>) {
    val b0 = b
    val b1: Any = b // box
    val b2: Any? = b // box
    val b3: InlineNullablePrimitive<T> = b
    val b4: InlineNullablePrimitive<T>? = b // box
}

fun <T: Any> test3(c: InlineNotNullReference<T>) {
    val c0 = c
    val c1: Any = c // box
    val c2: Any? = c // box
    val c3: InlineNotNullReference<T> = c
    val c4: InlineNotNullReference<T>? = c
}

fun <T> test4(d: InlineNullableReference<T>) {
    val d0 = d
    val d1: Any = d // box
    val d2: Any? = d // box
    val d3: InlineNullableReference<T> = d
    val d4: InlineNullableReference<T>? = d // box
}

fun <T: Any> test5(e: InlineNullableReference2<T>) {
    val e0 = e
    val e1: Any = e // box
    val e2: Any? = e // box
    val e3: InlineNullableReference2<T> = e
    val e4: InlineNullableReference2<T>? = e // box
}

fun box(): String {
    val a = InlineNotNullPrimitive(1)
    val b = InlineNullablePrimitive(1)
    val c = InlineNotNullReference("some")
    val d = InlineNullableReference("other")
    val e = InlineNullableReference2("other2")

    test1(a)
    test2(b)
    test3(c)
    test4(d)
    test5(e)

    return "OK"
}