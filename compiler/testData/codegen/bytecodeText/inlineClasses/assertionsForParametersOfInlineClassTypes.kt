// !LANGUAGE: +InlineClasses

inline class AsNonNullPrimitive(val i: Int)
inline class AsNonNullReference(val s: String)
// ^ 5 assertions (constructor, box method, erased constructor, 2 assertions in equals--impl)

fun nonNullPrimitive(a: AsNonNullPrimitive) {}

fun nonNullReference(b: AsNonNullReference) {} // assertion
fun AsNonNullReference.nonNullReferenceExtension(b1: AsNonNullReference) {} // 2 assertions

fun asNullablePrimitive(c: AsNonNullPrimitive?) {}
fun asNullableReference(c: AsNonNullReference?) {}

// 8 checkParameterIsNotNull