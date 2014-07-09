
inline fun <R, T> foo(x : R, y : R, block : (R) -> T) : T {
    val a = x is Number
    val b = x is Object

    val a1 = x as Number
    val b1 = x as Object

    if (a && b) {
        return block(x)
    } else {
        return block(y)
    }
}

fun bar() {
    foo(1, 2) { x -> x is Int }
}

// 0 valueOf
// 0 Value\s\(\)
// 2 INSTANCEOF
// 2 CHECKCAST
