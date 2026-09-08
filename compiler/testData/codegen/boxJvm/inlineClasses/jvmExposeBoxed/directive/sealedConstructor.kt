// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

// FILE: lib.kt
@JvmInline value class P(val n: Int)

sealed class Boxed(val p: P?) {
    class Impl(p: P?) : Boxed(p)
}

sealed class Unboxed(val p: P) {
    class Impl(p: P) : Unboxed(p)
}

// FILE: usage.kt
fun box(): String {
    val boxed = Boxed.Impl(P(1))
    if (boxed.p?.n != 1) return "FAIL boxed: " + boxed.p?.n

    val boxedNull = Boxed.Impl(null)
    if (boxedNull.p != null) return "FAIL boxedNull: " + boxedNull.p

    val unboxed = Unboxed.Impl(P(2))
    if (unboxed.p.n != 2) return "FAIL unboxed: " + unboxed.p.n

    return "OK"
}
