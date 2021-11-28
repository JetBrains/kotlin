// WITH_STDLIB
// IGNORE_BACKEND: JVM

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Result<T>(val isSuccess: Boolean)

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
