// !LANGUAGE: +InlineClasses

inline class AsNonNullPrimitive(val i: Int)
inline class AsNonNullReference(val s: String) // 3 assertions (constructor, box method, static constructor in $Erased classs)

fun nonNullPrimitive(a: AsNonNullPrimitive) {}

fun nonNullReference(b: AsNonNullReference) {} // assertion
fun AsNonNullReference.nonNullReferenceExtension(b1: AsNonNullReference) {} // 2 assertions

fun asNullablePrimitive(c: AsNonNullPrimitive?) {}
fun asNullableReference(c: AsNonNullReference?) {}

// 6 checkParameterIsNotNull