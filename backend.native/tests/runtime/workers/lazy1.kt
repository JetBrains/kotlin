package runtime.workers.lazy1

import kotlin.test.*

import kotlin.native.worker.*

class Leak {
    val leak by lazy { this }
}

@Test fun runTest() {
    assertFailsWith<InvalidMutabilityException> {
        for (i in 1..100)
            Leak().freeze().leak
    }
    println("OK")
}
