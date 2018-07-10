package runtime.workers.freeze4

import kotlin.test.*

import konan.worker.*

data class Data(val x: Int, val s: String, val next: Data? = null)

@Test fun runTest() {
    val data1 = Data(1, "")
    data1.freeze()
    assertFailsWith<FreezingException> {
        data1.ensureNeverFrozen()
    }

    val dataNF = Data(42, "42")
    dataNF.ensureNeverFrozen()
    val data2 = Data(2, "2", dataNF)
    assertFailsWith<FreezingException> {
        data2.freeze()
    }
    assert(!data2.isFrozen)
    println("OK")
}
