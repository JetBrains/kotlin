
inline fun <R, T> foo(x : R?, y : R?, block : (R?) -> T) : T {
    if (x == null) {
        return block(x)
    } else {
        return block(y)
    }
}

fun bar() {
    foo(1, 2) { x -> if (x != null) 1 else 2 }
}

// 0 valueOf
// 0 Value\s\(\)
// 1 IFNULL
// 0 IFNONNULL
