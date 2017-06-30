fun test1(a: String) = a in "alpha" .. "omega"
fun test2(a: String) = a !in "alpha" .. "omega"

// 0 INVOKESTATIC kotlin/ranges/RangesKt.rangeTo
// 0 INVOKEINTERFACE kotlin/ranges/ClosedRange.contains