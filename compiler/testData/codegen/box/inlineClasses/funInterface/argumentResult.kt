// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM

fun interface ResultHandler<T> {
    fun onResult(result: Result<T>)
}

fun doSmth(resultHandler: ResultHandler<Boolean>) {
    resultHandler.onResult(Result.success(true))
}

fun box(): String {
    var res = "FAIL"
    doSmth { result ->
        res = if (result.isSuccess) "OK" else "FAIL 1"
    }
    return res
}
