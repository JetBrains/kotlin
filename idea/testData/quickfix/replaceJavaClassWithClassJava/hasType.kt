// "Replace with '::class.java'" "true"
// WITH_RUNTIME
// DISABLE-ERRORS
fun main() {
    val c: Class<Int.Companion> = Int.javaClass<caret>
}