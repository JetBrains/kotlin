// IGNORE_BACKEND: JVM_IR

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
// 0 IFNULL
// 1 IFNONNULL
// 1 IF
