
inline fun calc<T, R>(value : T, fn: (T)->R) : R = fn(value)
inline fun <T> identity(value : T) : T = calc(value) {
    if (1 == 1) return it
    it
}

fun foo() {
    val x = identity(1)
}

// 1 GOTO
