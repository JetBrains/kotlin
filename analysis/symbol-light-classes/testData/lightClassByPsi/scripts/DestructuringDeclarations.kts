// KT-90199
data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

var (c, d) = X(2, "")

private val (e, f) = X(3, "")

val (@Deprecated("") g, @get:JvmName("hh") h) = X(4, "")
