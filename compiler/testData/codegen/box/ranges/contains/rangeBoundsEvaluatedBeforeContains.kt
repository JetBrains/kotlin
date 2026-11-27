// WITH_STDLIB
// KT-92004

var log = ""

fun lowerD(): Double { log += "L"; return 1.0 }
fun upperD(): Double { log += "U"; return 2.0 }
fun lowerF(): Float { log += "L"; return 1.0f }
fun upperF(): Float { log += "U"; return 2.0f }
fun upperI(): Int { log += "U"; return 2 }
fun upperL(): Long { log += "U"; return 2L }
fun upperU(): UInt { log += "U"; return 2u }
fun upperUL(): ULong { log += "U"; return 2uL }
fun upperC(): Char { log += "U"; return 'c' }

inline fun expectLog(at: String, expected: String, block: () -> Unit) {
    log = ""
    block()
    if (log != expected) throw AssertionError("$at: expected '$expected', actual '$log'")
}

inline fun expectUpperFailure(block: () -> Unit) {
    try {
        block()
    } catch (e: IllegalStateException) {
        if (e.message == "upper bound must be evaluated") return
        throw e
    }
    throw AssertionError("Upper bound was not evaluated")
}

class C(val n: Int) : Comparable<C> {
    override fun compareTo(other: C): Int {
        log += "C$n:${other.n};"
        return n.compareTo(other.n)
    }
}
fun lowerComparable(): C { log += "L"; return C(1) }
fun upperComparable(): C { log += "U"; return C(2) }

fun box(): String {
    expectUpperFailure { 0.0 in 1.0..error("upper bound must be evaluated") }
    expectUpperFailure { 0.0 !in 1.0..error("upper bound must be evaluated") }
    expectUpperFailure { 0.0 in 1.0..<error("upper bound must be evaluated") }
    expectUpperFailure { 0.0 !in 1.0..<error("upper bound must be evaluated") }

    expectLog("Double ..", "U") { 0.0 in 1.0..upperD() }
    expectLog("Double .. !in", "U") { 0.0 !in 1.0..upperD() }
    expectLog("Double ..<", "U") { 0.0 in 1.0..<upperD() }
    expectLog("Double ..< !in", "U") { 0.0 !in 1.0..<upperD() }
    expectLog("Float ..", "U") { 0.0f in 1.0f..upperF() }
    expectLog("Float ..<", "U") { 0.0f in 1.0f..<upperF() }
    expectLog("both Double ..", "LU") { 0.0 in lowerD()..upperD() }
    expectLog("both Double ..<", "LU") { 0.0 in lowerD()..<upperD() }
    expectLog("lower only", "L") { 0.0 in lowerD()..2.0 }
    expectLog("NaN", "U") { Double.NaN in 1.0..upperD() }
    expectLog("Int", "U") { 0 in 1..upperI() }
    expectLog("Long", "U") { 0L in 1L..upperL() }
    expectLog("UInt", "U") { 0u in 1u..upperU() }
    expectLog("ULong", "U") { 0uL in 1uL..upperUL() }
    expectLog("Char", "U") { 'a' in 'b'..upperC() }
    expectLog("reverse order", "U") { 0 in (upperI() downTo 1) }

    // Checking the lower bound must remain the first comparison, even if the bounds have side effects.
    val x = C(0)
    expectLog("Comparable ..", "LUC0:1;") { x in lowerComparable()..upperComparable() }
    expectLog("Comparable .. !in", "LUC0:1;") { x !in lowerComparable()..upperComparable() }
    expectLog("Comparable ..<", "LUC0:1;") { x in lowerComparable()..<upperComparable() }
    expectLog("Comparable ..< !in", "LUC0:1;") { x !in lowerComparable()..<upperComparable() }
    val lower = C(1)
    expectLog("Comparable upper only", "UC0:1;") { x in lower..upperComparable() }
    expectLog("Comparable open upper only", "UC0:1;") { x in lower..<upperComparable() }
    return "OK"
}
