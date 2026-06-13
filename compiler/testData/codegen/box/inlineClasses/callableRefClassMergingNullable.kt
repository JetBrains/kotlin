// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// ISSUE: KT-86123

// The problem this box tests deals with is that on the Wasm backend, non-nullable value classes
// wrapping primitive types were merged with their nullable versions, which do need to be boxed.

OPTIONAL_JVM_INLINE_ANNOTATION
value class W(val x: Long)

fun nonNullCapture(d: W): () -> String = { d.toString() }
fun nullableCapture(d: W?): () -> String = { d.toString() }

fun box(): String {
    val a = nonNullCapture(W(42))
    val b = nullableCapture(W(99))
    val c = nullableCapture(null)
    if (a() != "W(x=42)") return "FAIL a: ${a()}"
    if (b() != "W(x=99)") return "FAIL b: ${b()}"
    if (c() != "null") return "FAIL c: ${c()}"
    return "OK"
}
