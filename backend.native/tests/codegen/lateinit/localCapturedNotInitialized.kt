package codegen.lateinit.localCapturedNotInitialized

import kotlin.test.*

@Test fun runTest() {
    lateinit var s: String

    fun foo() = s

    try {
        println(foo())
    }
    catch (e: RuntimeException) {
        println("OK")
        return
    }
    println("Fail")
}