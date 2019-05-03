// PROBLEM: none
// WITH_RUNTIME

fun main() {
    registerHandler(handlers = *arrayOf(
        { _<caret> -> },
        { it -> }
    ))
}

fun registerHandler(vararg handlers: (String) -> Unit) {
    handlers.forEach { it.invoke("hello") }
}