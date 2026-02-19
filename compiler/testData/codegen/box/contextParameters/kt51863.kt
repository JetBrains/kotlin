// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

import kotlin.test.*

context(string: String, strings: List<String>)
fun test(x: Any, y: Int = 0): List<Any> = listOf(string, strings, x, y)

fun box(): String {
    with("context1") {
        with(listOf("context2")) {
            assertEquals(
                listOf("context1", listOf("context2"), listOf(1), 1),
                test(y = 1, x = listOf(1))
            )
        }
    }
    return "OK"
}