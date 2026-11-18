// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-91642

// MODULE: lib
// FILE: lib.kt
@JvmInline
value class NonNegative(val v: Int) {
    init {
        require(v >= 0)
    }
}

class NoDefault(val x: Int, @IntroducedAt("2") val n: NonNegative = NonNegative(0))

class IntroducedPlainInt(val n: NonNegative, @IntroducedAt("2") val y: Int = 0)

class Clash(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1))

class ClashTwice(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1), @IntroducedAt("3") val u: UInt = 2u)

open class OpenClash(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1))

class Outer(val o: String) {
    inner class InnerClash(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1))
}

// MODULE: main(lib)
// FILE: main.kt
class SubClash(s: String, n: NonNegative) : OpenClash(s, n)

fun box(): String {
    if (NoDefault(1, NonNegative(2)).n.v != 2 || NoDefault(1).n.v != 0) return "FAIL: NoDefault"
    if (IntroducedPlainInt(NonNegative(2), 3).y != 3) return "FAIL: IntroducedPlainInt"

    val clash = Clash("a", NonNegative(3))
    if (clash.s != "a" || clash.n.v != 3) return "FAIL: Clash"
    if (Clash().s != "s" || Clash().n.v != 1) return "FAIL: Clash defaults"

    val clashTwice = ClashTwice("a", NonNegative(3), 4u)
    if (clashTwice.s != "a" || clashTwice.n.v != 3 || clashTwice.u != 4u) return "FAIL: ClashTwice"

    val sub = SubClash("a", NonNegative(3))
    if (sub.s != "a" || sub.n.v != 3) return "FAIL: SubClash"

    val inner = Outer("o").InnerClash("a", NonNegative(3))
    if (inner.s != "a" || inner.n.v != 3) return "FAIL: InnerClash"

    val constructor: (String, NonNegative) -> Clash = ::Clash
    if (constructor("a", NonNegative(3)).n.v != 3) return "FAIL: ::Clash"

    return "OK"
}
