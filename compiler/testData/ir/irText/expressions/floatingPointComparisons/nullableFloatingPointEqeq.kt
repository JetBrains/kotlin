fun testDD(x: Double?, y: Double?) = x == y
fun testDF(x: Double?, y: Any?) = y is Float? && x == y
fun testDI(x: Double?, y: Any?) = y is Int? && x == y
fun testDI2(x: Any?, y: Any?) = x is Int? && y is Double && x == y