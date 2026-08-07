// LANGUAGE: +FullValueClasses
// JVM_ABI_GEN
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Box

value class Box<T> private constructor(private val element: T, private val name: String) {
    companion object {
        fun <T> of(element: T): Box<T> = Box(element, "box")
    }
}
