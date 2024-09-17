// LANGUAGE: +InlineClasses

inline class AsNonNullPrimitive(val i: Int)
inline class AsNonNullReference(val s: String) // assertion (erased constructor)

fun nonNullPrimitive(a: AsNonNullPrimitive) {}

fun nonNullReference(b: AsNonNullReference) {} // assertion
fun AsNonNullReference.nonNullReferenceExtension(b1: AsNonNullReference) {} // 2 assertions

fun asNullablePrimitive(c: AsNonNullPrimitive?) {}
fun asNullableReference(c: AsNonNullReference?) {}

// 0 checkParameterIsNotNull
// 4 checkNotNullParameter
