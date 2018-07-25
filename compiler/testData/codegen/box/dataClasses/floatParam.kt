// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

val NAN = Float.NaN

data class A(val x: Float)

fun box(): String {
    if (A(+0f) == A(-0f)) return "Fail: +0 == -0"
    if (A(+0f).hashCode() == A(-0f).hashCode()) return "Fail: hash(+0) == hash(-0)"

    if (A(NAN) != A(NAN)) return "Fail: NaN != NaN"
    if (A(NAN).hashCode() != A(NAN).hashCode()) return "Fail: hash(NaN) != hash(NaN)"

    val s = HashSet<A>()
    for (times in 1..5) {
        s.add(A(3.14f))
        s.add(A(+0f))
        s.add(A(-0f))
        s.add(A(-2.72f))
        s.add(A(NAN))
    }

    if (A(3.14f) !in s) return "Fail: 3.14 not found"
    if (A(+0f) !in s) return "Fail: +0 not found"
    if (A(-0f) !in s) return "Fail: -0 not found"
    if (A(-2.72f) !in s) return "Fail: -2.72 not found"
    if (A(NAN) !in s) return "Fail: NaN not found"

    return if (s.size == 5) "OK" else "Fail $s"
}
