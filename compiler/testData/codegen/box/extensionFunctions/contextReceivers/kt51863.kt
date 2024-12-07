// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

import kotlin.test.*

context(String, List<String>)
fun test(x: Any, y: Int = 0): List<Any> = listOf(this@String, this@List, x, y)

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