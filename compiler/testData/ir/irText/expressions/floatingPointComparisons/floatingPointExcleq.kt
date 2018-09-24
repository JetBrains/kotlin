fun test1d(x: Double, y: Double) = x != y
fun test2d(x: Double, y: Double?) = x != y
fun test3d(x: Double, y: Any) = x != y
fun test4d(x: Double, y: Number) = x != y
fun test5d(x: Double, y: Any) = y is Double && x != y
fun test6d(x: Any, y: Any) = x is Double && y is Double && x != y

fun test1f(x: Float, y: Float) = x != y
fun test2f(x: Float, y: Float?) = x != y
fun test3f(x: Float, y: Any) = x != y
fun test4f(x: Float, y: Number) = x != y
fun test5f(x: Float, y: Any) = y is Float && x != y
fun test6f(x: Any, y: Any) = x is Float && y is Float && x != y

// The following possibly should not compile (but so far it does)
// because of EQUALITY_NOT_APPLICABLE.
fun testFD(x: Any, y: Any) = x is Float && y is Double && x != y
fun testDF(x: Any, y: Any) = x is Double && y is Float && x != y