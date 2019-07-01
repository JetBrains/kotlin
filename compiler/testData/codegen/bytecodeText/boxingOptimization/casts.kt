
inline fun <R, T> foo(x : R?, block : (R?) -> T) : T {
    return block(x)
}

fun bar() {
    foo(1) { x -> x!!.toLong() }
    foo(1) { x -> x!!.toShort() }
    foo(1L) { x -> x!!.toByte() }
    foo(1L) { x -> x!!.toShort() }
    foo('a') { x -> x!!.toDouble() }
    foo(1.0) { x -> x!!.toByte() }
}

// 0 valueOf
// 0 Value\s\(\)
// 1 I2L
// 2 L2I
// 2 I2S
// 2 I2B
// 1 I2D
// 1 D2I
