fun foo(x: Any, y: Any) {}

val y = true
val z = 1L

fun box(): String {
    var q = "Failed"
    val v = if (y) { q = "OK"; z } else ""
    foo(v, return q)
}