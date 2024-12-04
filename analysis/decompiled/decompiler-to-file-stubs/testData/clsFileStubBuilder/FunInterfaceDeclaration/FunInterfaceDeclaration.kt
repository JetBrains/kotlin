package test

class FunInterfaceDeclaration {
    @Suppress("UNSUPPORTED_FEATURE")
    fun interface KRunnable {
        fun invoke()
    }

    @Suppress("UNSUPPORTED_FEATURE")
    fun interface GenericKRunnable<T, R> {
        fun invoke(t: T): R
    }
}
