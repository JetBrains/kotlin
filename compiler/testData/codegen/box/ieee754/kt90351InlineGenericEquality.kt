// WITH_STDLIB
// KT-90351

inline fun <T> eqInline(a: T, b: T) = a == b
fun <T> eqGeneric(a: T, b: T) = a == b
inline fun <T> eqSelector(a: Any, b: Any, sel: (Any) -> T) = sel(a) == sel(b)
inline fun <reified T> eqReified(a: T, b: T) = a == b

fun nanA(): Double = Double.NaN
fun nanB(): Double = Double.fromBits(0xFFF80000L shl 32)
fun floatNanA(): Float = Float.NaN
fun floatNanB(): Float = Float.fromBits(0xFFC00000.toInt())

fun box(): String {
    val a = nanA()
    val b = nanB()
    val floatA = floatNanA()
    val floatB = floatNanB()
    if (!a.isNaN() || !b.isNaN() || a.toRawBits() == b.toRawBits()) return "Fail: Double NaN bit patterns"
    if (!floatA.isNaN() || !floatB.isNaN() || floatA.toRawBits() == floatB.toRawBits()) return "Fail: Float NaN bit patterns"

    val boxedA: Any = a
    val boxedB: Any = b
    val failures = mutableListOf<String>()
    if (!eqInline(a, b)) failures += "Fail: inline Double"
    if (!eqInline(a, a)) failures += "Fail: inline same NaN bits"
    if (!eqGeneric(a, b)) failures += "Fail: generic Double"
    if (!eqInline(boxedA, boxedB)) failures += "Fail: inline Any"
    if (!eqSelector(boxedA, boxedB) { it }) failures += "Fail: inline selector"
    if (!eqReified(a, b)) failures += "Fail: reified Double"
    if (!eqInline(floatA, floatB)) failures += "Fail: inline Float"
    if (!eqInline<Double?>(a, b)) failures += "Fail: inline nullable Double"

    return failures.joinToString("; ").ifEmpty { "OK" }
}
