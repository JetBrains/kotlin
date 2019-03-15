// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class InlineNotNullPrimitive(val x: Int)
inline class InlineNotNullReference(val y: String)

fun <T> testNotNullPrimitive(a: Any, b: T, c: InlineNotNullPrimitive, d: InlineNotNullPrimitive?) {}
fun <T> testNotNullReference(a: Any, b: T, c: InlineNotNullReference, d: InlineNotNullReference?) {}

fun test(a: InlineNotNullPrimitive, b: InlineNotNullReference) {
    testNotNullPrimitive(a, a, a, a) // 3 box
    testNotNullReference(b, b, b, b) // 2 box
}

fun box(): String {
    val a = InlineNotNullPrimitive(10)
    val b = InlineNotNullReference("some")

    test(a, b)

    return "OK"
}