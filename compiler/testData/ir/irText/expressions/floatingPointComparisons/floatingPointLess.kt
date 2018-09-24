fun test1d(x: Double, y: Double) = x < y
fun test2d(x: Double, y: Any) = y is Double && x < y
fun test3d(x: Any, y: Any) = x is Double && y is Double && x < y

fun test1f(x: Float, y: Float) = x < y
fun test2f(x: Float, y: Any) = y is Float && x < y
fun test3f(x: Any, y: Any) = x is Float && y is Float && x < y

fun testFD(x: Any, y: Any) = x is Float && y is Double && x < y
fun testDF(x: Any, y: Any) = x is Double && y is Float && x < y
