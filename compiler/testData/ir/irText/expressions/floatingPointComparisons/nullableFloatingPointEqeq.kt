fun testDD(x: Double?, y: Double?) = x == y
fun testDF(x: Double?, y: Any?) = y is Float? && x == y
fun testDI(x: Double?, y: Any?) = y is Int? && x == y
// TODO: FE1.0 allows comparison of incompatible type after smart cast (KT-46383) but FIR rejects it. We need to figure out a transition plan.
//fun testDI2(x: Any?, y: Any?) = x is Int? && y is Double && x == y