// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class InlineNotNullPrimitive(val x: Int)
inline class InlineNullablePrimitive(val x: Int?)
inline class InlineNotNullReference(val a: Any)
inline class InlineNullableReference(val a: Any?)

fun test1(a: InlineNotNullPrimitive) {
    val a0 = a
    val a1: Any = a // box
    val a2: Any? = a // box
    val a3: InlineNotNullPrimitive = a
    val a4: InlineNotNullPrimitive? = a // box
}

fun test2(b: InlineNullablePrimitive) {
    val b0 = b
    val b1: Any = b // box
    val b2: Any? = b // box
    val b3: InlineNullablePrimitive = b
    val b4: InlineNullablePrimitive? = b // box
}

fun test3(c: InlineNotNullReference) {
    val c0 = c
    val c1: Any = c // box
    val c2: Any? = c // box
    val c3: InlineNotNullReference = c
    val c4: InlineNotNullReference? = c
}

fun test4(d: InlineNullableReference) {
    val d0 = d
    val d1: Any = d // box
    val d2: Any? = d // box
    val d3: InlineNullableReference = d
    val d4: InlineNullableReference? = d // box
}

fun box(): String {
    val a = InlineNotNullPrimitive(1)
    val b = InlineNullablePrimitive(1)
    val c = InlineNotNullReference("some")
    val d = InlineNullableReference("other")

    test1(a)
    test2(b)
    test3(c)
    test4(d)

    return "OK"
}