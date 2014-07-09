
inline fun <R, T> foo(x : R, block : (R) -> T) : T {
    var y = x
    var z = y
    z = x
    return block(z)
}

fun bar() {
    foo(1) { x -> x }
    foo(1f) { x -> x }
    foo(1L) { x -> x }
    foo(1.toDouble()) { x -> x }
    foo(1.toShort()) { x -> x }
    foo(1.toByte()) { x -> x }
    foo('a') { x -> x }
    foo(true) { x -> x }
}

// 0 valueOf
// 0 Value\s\(\)
