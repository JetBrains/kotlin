// JVM_ABI_GEN
// LIBRARY_PLATFORMS: JVM
// JVM_ABI_GEN

inline class Box<T> private constructor(private val element: T) {
    companion object {
        fun <T> makeThreadLocalBox(element: ThreadLocal<T>) = Box(element)
    }
}
