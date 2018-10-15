// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Result<out T>(val value: Any?) {
    val isFailure: Boolean get() = value is Failure

    public companion object {
        public inline fun <T> success(value: T): Result<T> =
            Result(value)

        public inline fun <T> failure(exception: Throwable): Result<T> =
            Result(Failure(exception))
    }

    class Failure (
        val exception: Throwable
    )
}

inline fun <R> runCatching(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

class Box<T>(val x: T)

fun box(): String {
    val r = runCatching { TODO() }
    val b = Box(r)
    if (r.isFailure != b.x.isFailure || !r.isFailure) return "Fail: r=${r.isFailure};  b.x=${b.x.isFailure}"

    return "OK"
}