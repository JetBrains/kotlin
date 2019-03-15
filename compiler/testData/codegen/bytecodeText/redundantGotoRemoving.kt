// IGNORE_BACKEND: JVM_IR
val nonConstFlag = true

inline fun <T, R> calc(value : T, fn: (T) -> R) : R = fn(value)

inline fun <T> identity(value : T) : T = calc(value) {
    if (nonConstFlag) return it
    it
}

fun foo() {
    val x = identity(1)
}

// 1 GOTO
