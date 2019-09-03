package foo

internal expect abstract class ExpectBase() {
    abstract fun foo(cause: Throwable?)
}

interface I {
    fun foo(cause: Throwable?)
}

internal abstract class CommonAbstract : ExpectBase()
