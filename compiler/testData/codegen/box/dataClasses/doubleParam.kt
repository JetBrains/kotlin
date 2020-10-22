// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

val NAN = Double.NaN

data class A(val x: Double)

fun box(): String {
    if (A(+0.0) == A(-0.0)) return "Fail: +0.0 == -0.0"
    if (A(+0.0).hashCode() == A(-0.0).hashCode()) return "Fail: hash(+0.0) == hash(-0.0)"

    if (A(NAN) != A(NAN)) return "Fail: NaN != NaN"
    if (A(NAN).hashCode() != A(NAN).hashCode()) return "Fail: hash(NaN) != hash(NaN)"

    val s = HashSet<A>()
    for (times in 1..5) {
        s.add(A(3.14))
        s.add(A(+0.0))
        s.add(A(-0.0))
        s.add(A(-2.72))
        s.add(A(NAN))
    }

    if (A(3.14) !in s) return "Fail: 3.14 not found"
    if (A(+0.0) !in s) return "Fail: +0.0 not found"
    if (A(-0.0) !in s) return "Fail: -0.0 not found"
    if (A(-2.72) !in s) return "Fail: -2.72 not found"
    if (A(NAN) !in s) return "Fail: NaN not found"

    return if (s.size == 5) "OK" else "Fail $s"
}
