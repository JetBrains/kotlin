package runtime.exceptions.extend0

import kotlin.test.*

class C : Exception("OK")

@Test fun runTest() {
    try {
        throw C()
    } catch (e: Throwable) {
        println(e.message!!)
    }
}