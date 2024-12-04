fun foo(x: Any, y: Any) {}

val y = false
val zInt = 1
val zLong = 1L

fun box(): String {
    var q = "Failed"
    foo(if (y) zInt else { q = "OK"; zLong }, return q)
}