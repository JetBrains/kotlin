// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Result<T>(val isSuccess: Boolean)

fun interface ResultHandler<T> {
    fun onResult(result: Result<T>)
}

fun doSmth(resultHandler: ResultHandler<Boolean>) {
    resultHandler.onResult(Result(true))
}

fun box(): String {
    var res = "FAIL"
    doSmth { result ->
        res = if (result.isSuccess) "OK" else "FAIL 1"
    }
    return res
}
