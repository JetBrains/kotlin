import kotlin.reflect.*

fun foo() {
    String::class.primaryConstructor
    ::foo.isExternal
    listOf(42)
}
