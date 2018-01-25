package codegen.mpp.mpp1

import kotlin.test.*

fun box() {
    assertEquals(A().B().fourtyTwo(), 42)
    assertEquals(seventeen(), 17)
}

expect class A {
    constructor()

    inner class B {
        fun fourtyTwo(): Int

        constructor()
    }
}

actual class A {
    actual inner class B actual constructor() {
        actual fun fourtyTwo() = 42
    }
}

actual fun seventeen() = 17
expect fun seventeen(): Int

@Test fun runTest() {
    box()
}
