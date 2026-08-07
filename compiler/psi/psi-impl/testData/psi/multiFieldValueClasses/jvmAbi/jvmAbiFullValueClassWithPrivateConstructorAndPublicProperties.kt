// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +FullValueClasses
// JVM_ABI_GEN

value class ThreadLocalDelegate<T> private constructor(val threadLocal: ThreadLocal<T>, val name: String) {
    companion object {
        fun <T> create(name: String): ThreadLocalDelegate<T> = ThreadLocalDelegate(ThreadLocal(), name)
    }
}
