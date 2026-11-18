// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// ISSUE: KT-91642

@JvmInline
value class NonNegative(val v: Int) {
    init {
        require(v >= 0)
    }
}

// The overload for the initial version has no default value, so it has no `$default` stub
class NoDefault(val x: Int, @IntroducedAt("2") val n: NonNegative = NonNegative(0))

data class DataNoDefault(val x: Int, @IntroducedAt("2") val n: NonNegative = NonNegative(0))

class IntroducedPlainInt(val n: NonNegative, @IntroducedAt("2") val y: Int = 0)

// `(String, int, DefaultConstructorMarker)` is the `$default` stub of `Clash(s: String = "s")`
class Clash(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1))

class ClashTwice(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1), @IntroducedAt("3") val u: UInt = 2u)

private val defaultConstructorMarker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")

// Java sees NonNegative as int, so it must not see a constructor that takes it unboxed
fun javaSeesRawConstructor(c: Class<*>, vararg raw: Class<*>): Boolean =
    c.constructors.any { !it.isSynthetic && it.parameterTypes.contentEquals(raw) }

fun box(): String {
    val int = Int::class.javaPrimitiveType!!
    if (javaSeesRawConstructor(NoDefault::class.java, int, int)) return "FAIL: NoDefault(int, int)"
    if (javaSeesRawConstructor(DataNoDefault::class.java, int, int)) return "FAIL: DataNoDefault(int, int)"
    if (javaSeesRawConstructor(IntroducedPlainInt::class.java, int, int)) return "FAIL: IntroducedPlainInt(int, int)"
    if (javaSeesRawConstructor(Clash::class.java, String::class.java, int)) return "FAIL: Clash(String, int)"
    if (javaSeesRawConstructor(ClashTwice::class.java, String::class.java, int, int)) return "FAIL: ClashTwice(String, int, int)"

    if (NoDefault(1, NonNegative(2)).n.v != 2 || NoDefault(1).n.v != 0) return "FAIL: NoDefault"
    if (DataNoDefault(1, NonNegative(2)).copy(x = 3) != DataNoDefault(3, NonNegative(2))) return "FAIL: DataNoDefault"
    if (IntroducedPlainInt(NonNegative(2), 3).y != 3 || IntroducedPlainInt(NonNegative(2)).y != 0) return "FAIL: IntroducedPlainInt"

    val clash = Clash("a", NonNegative(3))
    if (clash.s != "a" || clash.n.v != 3) return "FAIL: Clash explicit"
    if (Clash().s != "s" || Clash().n.v != 1) return "FAIL: Clash defaults"

    val clashTwice = ClashTwice("a", NonNegative(3), 4u)
    if (clashTwice.s != "a" || clashTwice.n.v != 3 || clashTwice.u != 4u) return "FAIL: ClashTwice explicit"
    if (ClashTwice("a").n.v != 1 || ClashTwice("a").u != 2u) return "FAIL: ClashTwice defaults"

    // How a binary compiled against `Clash(s: String = "s")` calls `Clash()`
    val oldDefaultCall = Clash::class.java.getConstructor(String::class.java, int, defaultConstructorMarker)
        .newInstance(null, 1, null) as Clash
    if (oldDefaultCall.s != "s" || oldDefaultCall.n.v != 1) return "FAIL: old Clash()"

    // How a binary compiled by 2.5.0, where `(String, int)` was the public constructor, calls `Clash("a", NonNegative(3))`
    val oldExplicitCall = Clash::class.java.getConstructor(String::class.java, int).newInstance("a", 3) as Clash
    if (oldExplicitCall.s != "a" || oldExplicitCall.n.v != 3) return "FAIL: old Clash explicit"

    return "OK"
}
