// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// See also kt33054.kt

fun causesVerifyErrorSample(): Sample<Boolean> = Sample
    .Success(true)
    .flatMap { Sample.Failure(RuntimeException()) }

sealed class Sample<out T> {
    inline fun <R> flatMap(f: (T) -> Sample<R>): Sample<R> =
        when (this) {
            is Failure -> this
            is Success -> f(this.value)
        }

    data class Failure(val exception: Throwable): Sample<Nothing>()
    data class Success<out T>(val value: T): Sample<T>()
}

fun box(): String {
    causesVerifyErrorSample()
    return "OK"
}
