package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")

    @Suppress("RESULT_CLASS_IN_RETURN_TYPE")
    operator fun plus(other: Result<T>) = other
}

