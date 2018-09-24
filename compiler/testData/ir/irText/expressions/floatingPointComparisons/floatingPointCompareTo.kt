fun test1d(x: Double, y: Double) = x.compareTo(y)
fun test2d(x: Double, y: Any) = y is Double && x.compareTo(y) == 0
fun test3d(x: Any, y: Any) = x is Double && y is Double && x.compareTo(y) == 0

fun test1f(x: Float, y: Float) = x.compareTo(y)
fun test2f(x: Float, y: Any) = y is Float && x.compareTo(y) == 0
fun test3f(x: Any, y: Any) = x is Float && y is Float && x.compareTo(y) == 0

fun testFD(x: Any, y: Any) = x is Float && y is Double && x.compareTo(y) == 0
fun testDF(x: Any, y: Any) = x is Double && y is Float && x.compareTo(y) == 0

fun Float.test1fr(x: Float) = compareTo(x)
fun Float.test2fr(x: Any) = x is Float && compareTo(x) == 0
fun Float.test3fr(x: Any) = x is Double && compareTo(x) == 0