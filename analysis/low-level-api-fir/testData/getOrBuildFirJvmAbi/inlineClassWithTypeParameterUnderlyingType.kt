// JVM_ABI_GEN
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Box

@JvmInline
value class Box<T> private constructor(private val element: T) {
    companion object {
        fun <T> of(element: T): Box<T> = Box(element)
    }
}
