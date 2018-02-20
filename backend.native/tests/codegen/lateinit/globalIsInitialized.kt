package codegen.lateinit.globalIsInitialized

import kotlin.test.*

lateinit var s: String

fun foo() {
    println(::s.isInitialized)
}

@Test fun runTest() {
    foo()
    s = "zzz"
    foo()
}