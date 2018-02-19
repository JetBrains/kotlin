package codegen.lateinit.localInitialized

import kotlin.test.*

@Test fun runTest() {
    lateinit var s: String
    s = "zzz"
    println(s)
}