fun foo(x: Any, y: Any) {}

val y = false
val z = 1L

fun box(): String {
    var q = "Failed"
    foo(if (y) "" else { q = "OK"; z }, return q)
}