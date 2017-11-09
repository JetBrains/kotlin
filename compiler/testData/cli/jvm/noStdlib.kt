import kotlin.reflect.*

fun foo() {
    String::class.primaryConstructor
    Void::class
    ::foo.isExternal
    listOf(42)
}
