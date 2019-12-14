// "Replace with 'Companion::class.java'" "true"
// WITH_RUNTIME
fun main() {
    val c = Int.javaClass<caret>
}