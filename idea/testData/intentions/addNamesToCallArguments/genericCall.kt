fun foo(a: String) {}
inline fun <reified T> generic() = null as T

fun main() {
    <caret>foo(generic())
}