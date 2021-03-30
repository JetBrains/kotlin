// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JVM

inline class Result<T>(val isSuccess: Boolean)

fun interface ResultHandler<T> {
    fun onResult(): Result<T>
}

fun doSmth(resultHandler: ResultHandler<Boolean>): Result<Boolean> {
    return resultHandler.onResult()
}

fun box(): String {
    var res = doSmth { Result(true) }
    return if (res.isSuccess) "OK" else "FAIL"
}
