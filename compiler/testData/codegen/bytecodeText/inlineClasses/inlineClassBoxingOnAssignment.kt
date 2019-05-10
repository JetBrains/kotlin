// !LANGUAGE: +InlineClasses

inline class InlineNotNullPrimitive(val x: Int)
inline class InlineNullablePrimitive(val x: Int?)
inline class InlineNotNullReference(val a: Any)
inline class InlineNullableReference(val a: Any?)

fun test1(a: InlineNotNullPrimitive) {
    val a0 = a
    val a1: Any = a
    val a2: Any? = a
    val a3: InlineNotNullPrimitive = a
    val a4: InlineNotNullPrimitive? = a
}

fun test2(b: InlineNullablePrimitive) {
    val b0 = b
    val b1: Any = b
    val b2: Any? = b
    val b3: InlineNullablePrimitive = b
    val b4: InlineNullablePrimitive? = b
}

fun test3(c: InlineNotNullReference) {
    val c0 = c
    val c1: Any = c
    val c2: Any? = c
    val c3: InlineNotNullReference = c
    val c4: InlineNotNullReference? = c
}

fun test4(d: InlineNullableReference) {
    val d0 = d
    val d1: Any = d
    val d2: Any? = d
    val d3: InlineNullableReference = d
    val d4: InlineNullableReference? = d
}

// 0 INVOKESTATIC InlineNotNullPrimitive\$Erased.box
// 0 INVOKESTATIC InlineNullablePrimitive\$Erased.box
// 0 INVOKESTATIC InlineNotNullReference\$Erased.box
// 0 INVOKESTATIC InlineNullableReference\$Erased.box

// 0 valueOf