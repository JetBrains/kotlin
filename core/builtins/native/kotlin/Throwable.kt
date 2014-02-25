package kotlin

public open class Throwable(message: String? = null, cause: Throwable? = null) {
    public fun getMessage(): String?

    public fun getCause(): Throwable?

    public fun printStackTrace(): Unit
}
