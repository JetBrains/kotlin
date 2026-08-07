// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_ABI_GEN

@JvmInline
value class ThreadLocalDelegate<T>(private val threadLocal: ThreadLocal<T>)
