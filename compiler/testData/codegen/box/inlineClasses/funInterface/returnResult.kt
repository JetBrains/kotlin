// WITH_STDLIB
// IGNORE_BACKEND: JVM

fun interface ResultHandler<T> {
    @Suppress("RESULT_CLASS_IN_RETURN_TYPE")
    fun onResult(): Result<T>
}

@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
fun doSmth(resultHandler: ResultHandler<Boolean>): Result<Boolean> {
    return resultHandler.onResult()
}

fun box(): String {
    var res = doSmth { Result.success(true) }
    return if (res.isSuccess) "OK" else "FAIL 1"
}
