// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

class Result<T>(val x: T)

context(_context0: Result<T>)
val <T> result: Result<T> get() = _context0

fun <T> Result<T>.x(): T {
    with(result) {
        return x
    }
}

fun box(): String {
    with(Result<String>("OK")) {
        return x()
    }
}
