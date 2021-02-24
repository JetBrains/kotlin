// IS_APPLICABLE: false
// WITH_RUNTIME
annotation class Foo(val value: String)

fun main() {
    val l = listOf<@Foo("bar") <caret>String>("")
}