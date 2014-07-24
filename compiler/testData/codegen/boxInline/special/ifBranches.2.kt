package test

inline fun <T> runIf(f: (T) -> T, start: T, stop: T, secondStart: T) : T {
    if (f(start) == stop) {
        return f(start)
    }
    return f(secondStart)
}


inline fun <T> runIf2(f: (T) -> T, start: T, stop: T, secondStart: T) : T {
    val result = f(start)
    if (result == stop) {
        return result
    }
    return f(secondStart)
}

inline fun <T> runIfElse(f: (T) -> T, start: T, stop: T, secondStart: T) : T {
    if (f(start) == stop) {
        return f(start)
    } else {
        return f(secondStart)
    }
}