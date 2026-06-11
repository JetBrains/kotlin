// WITH_STDLIB
// KT-86678: boxed Double NaN values must compare equal via ==/equals.

fun box(): String {
    val a = Double.NaN                          // 0x7FF8000000000000
    val b = Double.fromBits(0xFFF80000L shl 32) // 0xFFF8000000000000 (also NaN)

    if (!a.isNaN() || !b.isNaN()) return "Fail: operands not NaN"

    // Primitive IEEE comparison: NaN != NaN.
    if (a == b) return "Fail: primitive == should be false"
    // equals/compareTo treat NaNs as equal.
    if (a.compareTo(b) != 0) return "Fail: compareTo"
    if (!a.equals(b)) return "Fail: equals"

    val boxedA: Any = a
    val boxedB: Any = b
    if (!boxedA.equals(boxedB)) return "Fail: boxed equals"
    if (boxedA != boxedB) return "Fail: boxed =="

    return "OK"
}
