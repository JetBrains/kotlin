// JVM_ABI_GEN
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: ThreadLocalDelegate

@JvmInline
value class ThreadLocalDelegate<T> private constructor(private val threadLocal: ThreadLocal<T>) {
    companion object {
        fun <T> create(): ThreadLocalDelegate<T> = ThreadLocalDelegate(ThreadLocal())
    }
}
