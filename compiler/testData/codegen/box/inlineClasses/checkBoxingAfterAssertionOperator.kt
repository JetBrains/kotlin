// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class WithPrimitive(val a: Int)
fun takeWithPrimitive(a: WithPrimitive) {}

inline class WithReference(val a: Any)
fun takeWithReference(a: WithReference) {}

inline class WithNullableReference(val a: Any?)
fun takeWithNullableReference(a: WithNullableReference) {}

fun foo(a: WithPrimitive?, b: WithPrimitive) {
    takeWithPrimitive(a!!) // unbox
    takeWithPrimitive(a) // unbox
    takeWithPrimitive(b!!)
}

fun bar(a: WithReference?, b: WithReference) {
    takeWithReference(a!!)
    takeWithReference(a)
    takeWithReference(b!!)
}

fun baz(a: WithNullableReference?, b: WithNullableReference) {
    takeWithNullableReference(a!!) // unbox
    takeWithNullableReference(a) // unbox
    takeWithNullableReference(a!!) // unbox
    takeWithNullableReference(b!!)
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

    return "OK"
}