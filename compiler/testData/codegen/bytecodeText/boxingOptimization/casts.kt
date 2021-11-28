
inline fun <R, T> foo(x : R?, block : (R?) -> T) : T {
    return block(x)
}

fun bar() {
    val a = foo(1) { x -> x!!.toLong() }
    val b = foo(1) { x -> x!!.toShort() }
    val c = foo(1L) { x -> x!!.toByte() }
    val d = foo(1L) { x -> x!!.toShort() }
    val e = foo('a') { x -> x!!.toDouble() }
    val f = foo(1.0) { x -> x!!.toInt() }
}

// 0 valueOf
// 0 Value\s\(\)
// 1 I2L
// 2 L2I
// 2 I2S
// 1 I2B
// 1 I2D
// 1 D2I
