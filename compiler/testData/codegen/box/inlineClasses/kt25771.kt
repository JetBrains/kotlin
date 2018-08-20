// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class SuccessOrFailure<out T>(val value: Any?) {
    val isFailure: Boolean get() = value is Failure

    public companion object {
        public inline fun <T> success(value: T): SuccessOrFailure<T> =
            SuccessOrFailure(value)

        public inline fun <T> failure(exception: Throwable): SuccessOrFailure<T> =
            SuccessOrFailure(Failure(exception))
    }

    class Failure (
        val exception: Throwable
    )
}

inline fun <R> runCatching(block: () -> R): SuccessOrFailure<R> {
    return try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }
}

class Box<T>(val x: T)

fun box(): String {
    val r = runCatching { TODO() }
    val b = Box(r)
    if (r.isFailure != b.x.isFailure || !r.isFailure) return "Fail: r=${r.isFailure};  b.x=${b.x.isFailure}"

    return "OK"
}