package runtime.workers.freeze3

import kotlin.test.*

import konan.worker.*

object Immutable {
    var x = 1
}

@konan.ThreadLocal
object Mutable {
    var x = 2
}

@Test fun runTest() {
    assertEquals(1, Immutable.x)
    assertFailsWith<InvalidMutabilityException> {
        Immutable.x++
    }
    assertEquals(1, Immutable.x)
    Mutable.x++
    assertEquals(3, Mutable.x)
    println("OK")
}
