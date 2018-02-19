package codegen.lateinit.localCapturedInitialized

import kotlin.test.*

@Test fun runTest() {
    lateinit var s: String

    fun foo() = s

    s = "zzz"
    println(foo())
}