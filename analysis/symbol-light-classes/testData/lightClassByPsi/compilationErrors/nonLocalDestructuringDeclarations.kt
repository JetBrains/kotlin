// KT-90199
data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

var (c, d) = X(2, "")

private val (e, f) = X(3, "")

val (@Deprecated("") g, @get:JvmName("hh") h) = X(4, "")

val z = 0

class C {
    val (a, b) = X(1, "")

    var (c, d) = X(2, "")

    private val (e, f) = X(3, "")

    val (@Deprecated("") g, @get:JvmName("hh") h) = X(4, "")

    companion object {
        val (i, j) = X(5, "")
    }
}

object O {
    val (a, b) = X(1, "")
}
