// LANGUAGE: +InlineClasses

inline class AsNonNullPrimitive(val i: Int)
inline class AsNonNullReference(val s: String)
// ^ JVM: 3 assertions (constructor, box method, erased constructor)
//   JVM IR: 1 assertion (erased constructor)

fun nonNullPrimitive(a: AsNonNullPrimitive) {}

fun nonNullReference(b: AsNonNullReference) {} // assertion
fun AsNonNullReference.nonNullReferenceExtension(b1: AsNonNullReference) {} // 2 assertions

fun asNullablePrimitive(c: AsNonNullPrimitive?) {}
fun asNullableReference(c: AsNonNullReference?) {}

// JVM_TEMPLATES
// 0 checkParameterIsNotNull
// 6 checkNotNullParameter

// JVM_IR_TEMPLATES
// 0 checkParameterIsNotNull
// 4 checkNotNullParameter
