// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +FullValueClasses
// JVM_ABI_GEN

value class ThreadLocalDelegate<T>(val threadLocal: ThreadLocal<T>, val name: String)
