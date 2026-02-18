// NO_CHECK_LAMBDA_INLINING
// FILE: main.kt
fun box(): String {
    tryAndLog {
        throw RuntimeException()
    }
    return "OK"
}

// FILE: lib.kt
typealias EmptyFunctionResult<T> = () -> T

typealias LoggingFunctionType<T> = (tag: String, message: String, throwable: Throwable?) -> T

inline fun <T> tryAndLog(
    title: String = "",
    message: String = "",
    logger: LoggingFunctionType<*> = L::error,
    throwableAction: EmptyFunctionResult<T>
): T? {
    return try {
        throwableAction()
    } catch (e: Throwable) {
        logger(title, message, e)
        return null
    }
}

open class LLogger {
    fun error(tag: String, message: String, exception: Throwable?): Unit {}
}

object L : LLogger()
