import kotlin.reflect.*

fun foo() {
    String::class.primaryConstructor
    Void::class
    ::foo.name
    listOf(42)
}
