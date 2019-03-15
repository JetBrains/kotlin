// PROBLEM: none
// WITH_RUNTIME

fun main() {
    registerHandler(handlers = *arrayOf(
        { _ -> },
        { it<caret> -> }
    ))
}

fun registerHandler(vararg handlers: (String) -> Unit) {
    handlers.forEach { it.invoke("hello") }
}