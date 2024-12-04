fun foo(x: Any, y: Any) {}

val y = true
val zByte = 1.toByte()
val zShort = 1.toShort()

fun box(): String {
    var q = "Failed"
    foo(if (y) { q = "OK"; zByte } else zShort, return q)
}