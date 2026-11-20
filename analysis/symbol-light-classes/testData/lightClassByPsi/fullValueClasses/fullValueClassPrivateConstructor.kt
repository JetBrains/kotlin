// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package pack

value class ThreadLocalDelegate<T> private constructor(private val threadLocal: ThreadLocal<T>, private val name: String) {
    companion object {
        fun <T> create(name: String): ThreadLocalDelegate<T> = ThreadLocalDelegate(ThreadLocal(), name)
    }
}
