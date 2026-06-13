// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// ISSUE: KT-86123

// Tests that callable reference class deduplication handles generic type parameters correctly
// when they have different upper bounds (nullable vs non-nullable, primitive-wrapping value class, etc.)

OPTIONAL_JVM_INLINE_ANNOTATION
value class W(val x: Long)

// T has implicit bound Any? (nullable) => erases to anyNType
fun <T> captureNullableBound(t: T): () -> String = { t.toString() }

// T : Any (non-nullable) => erases to anyNType (Any is a reference type)
fun <T : Any> captureNonNullBound(t: T): () -> String = { t.toString() }

// T : W (value class bound wrapping primitive) => erases to W (unboxed)
fun <T : W> captureValueClassBound(t: T): () -> String = { t.toString() }

// Two different type parameter names with same bound should merge
fun <A> captureA(a: A): () -> String = { a.toString() }
fun <B> captureB(b: B): () -> String = { b.toString() }

fun box(): String {
    // Nullable bound captures
    val n1 = captureNullableBound("hello")
    val n2 = captureNullableBound(42)
    val n3 = captureNullableBound<String?>(null)
    if (n1() != "hello") return "FAIL n1: ${n1()}"
    if (n2() != "42") return "FAIL n2: ${n2()}"
    if (n3() != "null") return "FAIL n3: ${n3()}"

    // Non-null bound captures
    val nn1 = captureNonNullBound("world")
    val nn2 = captureNonNullBound(99)
    if (nn1() != "world") return "FAIL nn1: ${nn1()}"
    if (nn2() != "99") return "FAIL nn2: ${nn2()}"

    // Value class bound captures
    val v1 = captureValueClassBound(W(77))
    if (v1() != "W(x=77)") return "FAIL v1: ${v1()}"

    // Different type parameter names, same bound => should merge correctly
    val a = captureA("from A")
    val b = captureB("from B")
    if (a() != "from A") return "FAIL a: ${a()}"
    if (b() != "from B") return "FAIL b: ${b()}"

    return "OK"
}
