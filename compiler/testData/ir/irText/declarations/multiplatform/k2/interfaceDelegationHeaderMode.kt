// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

interface ErrorCatching {
    val isEnabled: Boolean
    var retryCount: Int
    fun hasError(): Boolean
    fun reportError(error: Throwable)
    class Impl : ErrorCatching {
        override val isEnabled: Boolean = true
        override var retryCount: Int = 0
        override fun hasError(): Boolean = false
        override fun reportError(error: Throwable) {}
    }
}

interface Closeable {
    fun close()
    class Impl : Closeable {
        override fun close() {}
    }
}

expect open class TestBase() : ErrorCatching {
    override val isEnabled: Boolean
    override var retryCount: Int
    override fun hasError(): Boolean
    override fun reportError(error: Throwable)
}

expect class MultiDelegate(errorCatching: ErrorCatching.Impl, closeable: Closeable.Impl) : ErrorCatching, Closeable {
    override val isEnabled: Boolean
    override var retryCount: Int
    override fun hasError(): Boolean
    override fun reportError(error: Throwable)
    override fun close()
}

// MODULE: platform()()(common)
// HEADER_MODE
// FILE: platform.kt

actual open class TestBase(private val errorCatching: ErrorCatching.Impl) : ErrorCatching by errorCatching {
    actual constructor() : this(ErrorCatching.Impl())
}

actual class MultiDelegate actual constructor(
    errorCatching: ErrorCatching.Impl,
    closeable: Closeable.Impl
) : ErrorCatching by errorCatching, Closeable by closeable
