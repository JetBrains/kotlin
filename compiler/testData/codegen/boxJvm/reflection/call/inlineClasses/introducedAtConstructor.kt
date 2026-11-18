// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-91642

@JvmInline
value class NonNegative(val v: Int) {
    init {
        require(v >= 0)
    }
}

class NoDefault(val x: Int, @IntroducedAt("2") val n: NonNegative = NonNegative(0))

class Clash(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1))

class ClashTwice(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1), @IntroducedAt("3") val u: UInt = 2u)

class Outer(val o: String) {
    inner class InnerClash(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1))
}

fun box(): String {
    if (::NoDefault.call(1, NonNegative(2)).n.v != 2) return "FAIL: NoDefault call"
    val noDefault = ::NoDefault.callBy(mapOf(::NoDefault.parameters[0] to 1))
    if (noDefault.n.v != 0) return "FAIL: NoDefault callBy"

    val clash = ::Clash.call("a", NonNegative(3))
    if (clash.s != "a" || clash.n.v != 3) return "FAIL: Clash call"
    val clashDefaults = ::Clash.callBy(emptyMap())
    if (clashDefaults.s != "s" || clashDefaults.n.v != 1) return "FAIL: Clash callBy"

    val clashTwice = ::ClashTwice.call("a", NonNegative(3), 4u)
    if (clashTwice.s != "a" || clashTwice.n.v != 3 || clashTwice.u != 4u) return "FAIL: ClashTwice call"
    val clashTwiceDefaults = ::ClashTwice.callBy(mapOf(::ClashTwice.parameters[0] to "a"))
    if (clashTwiceDefaults.s != "a" || clashTwiceDefaults.n.v != 1 || clashTwiceDefaults.u != 2u) return "FAIL: ClashTwice callBy"

    val inner = Outer("o")::InnerClash.call("a", NonNegative(3))
    if (inner.s != "a" || inner.n.v != 3) return "FAIL: InnerClash call"

    return "OK"
}
