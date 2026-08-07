// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_ABI_GEN

@JvmInline
value class ThreadLocalDelegate<T> private constructor(private val threadLocal: ThreadLocal<T>) {
    companion object {
        fun <T> create(): ThreadLocalDelegate<T> = ThreadLocalDelegate(ThreadLocal())
    }
}
