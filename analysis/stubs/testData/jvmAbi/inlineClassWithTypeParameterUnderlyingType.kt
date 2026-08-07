// JVM_ABI_GEN

@JvmInline
value class Box<T> private constructor(private val element: T) {
    companion object {
        fun <T> of(element: T): Box<T> = Box(element)
    }
}
