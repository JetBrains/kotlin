@JvmInline
value class ThreadLocalDelegate<T>(private val threadLocal: ThreadLocal<T>)
