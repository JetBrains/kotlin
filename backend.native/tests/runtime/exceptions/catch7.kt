package runtime.exceptions.catch7

import kotlin.test.*

@Test fun runTest() {
    try {
        foo()
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            println(message)
        }
    }
}

fun foo() {
    throw Error("Error happens")
}