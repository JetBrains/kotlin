// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +FullValueClasses
// JVM_ABI_GEN

value class ThreadLocalDelegate<T>(private val threadLocal: ThreadLocal<T>, private val name: String)
