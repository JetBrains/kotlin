// WITH_STDLIB
// KT-85445: `x in a..<b` is lowered to `a <= x && x < b` for floating-point and Comparable open-end ranges.
// Check that the lowered form agrees with OpenEndRange.contains() for all edge cases.

var log = ""

fun <T> logged(tag: String, value: T): T {
    log += tag
    return value
}

class Logged(val value: Int) : Comparable<Logged> {
    override fun compareTo(other: Logged): Int {
        log += "c"
        return value.compareTo(other.value)
    }
}

enum class E { A, B, C, D }

fun doubleIn(x: Double, a: Double, b: Double) = x in a..<b
fun doubleNotIn(x: Double, a: Double, b: Double) = x !in a..<b
fun doubleRef(x: Double, a: Double, b: Double): Boolean { val r = a..<b; return r.contains(x) }

fun floatIn(x: Float, a: Float, b: Float) = x in a..<b
fun floatNotIn(x: Float, a: Float, b: Float) = x !in a..<b
fun floatRef(x: Float, a: Float, b: Float): Boolean { val r = a..<b; return r.contains(x) }

fun floatInDoubleIn(x: Float, a: Double, b: Double) = x in a..<b
fun floatInDoubleNotIn(x: Float, a: Double, b: Double) = x !in a..<b
fun floatInDoubleRef(x: Float, a: Double, b: Double): Boolean { val r = a..<b; return r.contains(x) }

fun stringIn(x: String, a: String, b: String) = x in a..<b
fun stringNotIn(x: String, a: String, b: String) = x !in a..<b
fun stringRef(x: String, a: String, b: String): Boolean { val r = a..<b; return r.contains(x) }

fun <T : Comparable<T>> genericIn(x: T, a: T, b: T) = x in a..<b
fun <T : Comparable<T>> genericNotIn(x: T, a: T, b: T) = x !in a..<b
fun <T : Comparable<T>> genericRef(x: T, a: T, b: T): Boolean { val r = a..<b; return r.contains(x) }

fun <T : Enum<T>> enumIn(x: T, a: T, b: T) = x in a..<b
fun <T : Enum<T>> enumRef(x: T, a: T, b: T): Boolean { val r = a..<b; return r.contains(x) }

fun box(): String {
    val doubles = listOf(
        Double.NaN, Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.0, -0.0, 0.0,
        Double.MIN_VALUE, 1.0, 2.0, Double.MAX_VALUE, Double.POSITIVE_INFINITY
    )
    for (i in doubles.indices) for (j in doubles.indices) for (k in doubles.indices) {
        val x = doubles[i]
        val a = doubles[j]
        val b = doubles[k]
        val id = "$i $j $k"
        val expected = doubleRef(x, a, b)
        if (doubleIn(x, a, b) != expected) return "Fail Double in: $id"
        if (doubleNotIn(x, a, b) != !expected) return "Fail Double !in: $id"
        if (genericIn(x, a, b) != genericRef(x, a, b)) return "Fail generic Double in: $id"
        if (genericNotIn(x, a, b) != !genericRef(x, a, b)) return "Fail generic Double !in: $id"

        val xf = x.toFloat()
        val af = a.toFloat()
        val bf = b.toFloat()
        val expectedF = floatRef(xf, af, bf)
        if (floatIn(xf, af, bf) != expectedF) return "Fail Float in: $id"
        if (floatNotIn(xf, af, bf) != !expectedF) return "Fail Float !in: $id"
        if (genericIn(xf, af, bf) != genericRef(xf, af, bf)) return "Fail generic Float in: $id"

        val expectedFD = floatInDoubleRef(xf, a, b)
        if (floatInDoubleIn(xf, a, b) != expectedFD) return "Fail Float in Double range: $id"
        if (floatInDoubleNotIn(xf, a, b) != !expectedFD) return "Fail Float !in Double range: $id"
    }

    // Spot checks against hand-written expectations.
    if (!doubleIn(0.0, 0.0, 1.0)) return "Fail: lower bound is inclusive"
    if (doubleIn(1.0, 0.0, 1.0)) return "Fail: upper bound is exclusive"
    if (!doubleIn(-0.0, 0.0, 1.0)) return "Fail: -0.0 == 0.0 for primitive comparisons"
    if (doubleIn(0.5, 1.0, 0.0)) return "Fail: reversed range is empty"
    if (doubleIn(1.0, 1.0, 1.0)) return "Fail: a..<a is empty"
    if (doubleIn(Double.NaN, 0.0, 1.0)) return "Fail: NaN is never in a Double range"
    if (doubleIn(0.5, Double.NaN, 1.0)) return "Fail: NaN lower bound"
    if (doubleIn(0.5, 0.0, Double.NaN)) return "Fail: NaN upper bound"
    if (!floatIn(Float.MAX_VALUE, 0.0f, Float.POSITIVE_INFINITY)) return "Fail: Float.MAX_VALUE < Infinity"
    if (floatIn(Float.POSITIVE_INFINITY, 0.0f, Float.POSITIVE_INFINITY)) return "Fail: Infinity is excluded"

    val strings = listOf("", "a", "alpha", "b", "omega", "z")
    for (x in strings) for (a in strings) for (b in strings) {
        val expected = stringRef(x, a, b)
        if (stringIn(x, a, b) != expected) return "Fail String in: '$x' '$a' '$b'"
        if (stringNotIn(x, a, b) != !expected) return "Fail String !in: '$x' '$a' '$b'"
    }
    if (!stringIn("alpha", "alpha", "omega")) return "Fail: String lower bound is inclusive"
    if (stringIn("omega", "alpha", "omega")) return "Fail: String upper bound is exclusive"

    val enums = E.values().toList()
    for (x in enums) for (a in enums) for (b in enums) {
        if (enumIn(x, a, b) != enumRef(x, a, b)) return "Fail enum in: $x $a $b"
    }
    if (enumIn(E.C, E.A, E.C)) return "Fail: enum upper bound is exclusive"
    if (!enumIn(E.A, E.A, E.C)) return "Fail: enum lower bound is inclusive"

    // Evaluation order and count: bounds first (A, then B), then the argument (X), each exactly once;
    // compareTo calls must match the ones made by OpenEndRange.contains().
    for (xv in 0..3) for (av in 0..3) for (bv in 0..3) {
        log = ""
        val actual = logged("X", Logged(xv)) in logged("A", Logged(av))..<logged("B", Logged(bv))
        val actualLog = log
        log = ""
        val range = logged("A", Logged(av))..<logged("B", Logged(bv))
        val expected = range.contains(logged("X", Logged(xv)))
        val expectedLog = log
        if (actual != expected) return "Fail Comparable in: $xv $av $bv"
        if (actualLog != expectedLog) return "Fail Comparable in order: $xv $av $bv: '$actualLog' != '$expectedLog'"

        log = ""
        val actualNot = logged("X", Logged(xv)) !in logged("A", Logged(av))..<logged("B", Logged(bv))
        if (actualNot != !expected) return "Fail Comparable !in: $xv $av $bv"
        if (log != expectedLog) return "Fail Comparable !in order: $xv $av $bv: '$log' != '$expectedLog'"
    }

    log = ""
    val d = logged("X", 1.5) in logged("A", 1.0)..<logged("B", 2.0)
    if (!d || log != "ABX") return "Fail Double order: '$log'"
    log = ""
    val dn = logged("X", 2.0) !in logged("A", 1.0)..<logged("B", 2.0)
    if (!dn || log != "ABX") return "Fail Double !in order: '$log'"
    log = ""
    val s = logged("X", "b") in logged("A", "a")..<logged("B", "c")
    if (!s || log != "ABX") return "Fail String order: '$log'"

    return "OK"
}
