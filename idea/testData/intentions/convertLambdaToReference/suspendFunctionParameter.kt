// IS_APPLICABLE: false
// LANGUAGE_VERSION: 1.3

fun coroutine(block: suspend () -> Unit) {}

suspend fun testAction(obj: Any, action: suspend (Any) -> Unit) {
    action(action)
}

fun println(message: Any?) {}

fun main() = coroutine {
    testAction("OK") {<caret>
        println(it)
    }
}