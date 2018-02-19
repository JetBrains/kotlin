package codegen.lateinit.localNotInitialized

import kotlin.test.*

@Test fun runTest() {
    lateinit var s: String

    try {
        println(s)
    }
    catch (e: RuntimeException) {
        println("OK")
        return
    }
    println("Fail")
}