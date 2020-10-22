// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
inline class Result<out T>(val value: Any?) {
    fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    public companion object {
        public inline fun <T> success(value: T): Result<T> =
            Result(value)

        public inline fun <T> failure(exception: Throwable): Result<T> =
            Result(Failure(exception))
    }

    class Failure(
        val exception: Throwable
    )
}

inline fun <T, R> T.runCatching(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }
}


inline fun <R, T : R> Result<T>.getOrElse(onFailure: (exception: Throwable) -> R): R {
    return when (val exception = exceptionOrNull()) {
        null -> value as T
        else -> onFailure(exception)
    }
}


class A {
    fun f() = runCatching { "OK" }.getOrElse { throw it }
}

fun box(): String = A().f()
