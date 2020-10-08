// "Replace with 'Companion::class.java'" "true"
// WITH_RUNTIME
fun main() {
    val name = Int.javaClass<caret>.name
}