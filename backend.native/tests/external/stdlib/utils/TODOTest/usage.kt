import kotlin.*
import kotlin.test.*

private class PartiallyImplementedClass {
    public val prop: String get() = TODO()
    fun method1() = TODO() as String
    public fun method2(): Int = TODO()

    public fun method3(switch: Boolean, value: String): String {
        if (!switch)
            TODO("what if false")
        else {
            if (value.length < 3)
                throw TODO("write message")
        }

        return value
    }

    public fun method4() {
        TODO()
    }
}

private fun assertNotImplemented(block: () -> Unit) {
    assertFailsWith<NotImplementedError>(block = block)
}

private fun assertNotImplementedWithMessage(message: String, block: () -> Unit) {
    val e = assertFailsWith<NotImplementedError>(block = block)
    assertTrue(message in e.message!!)
}


fun box() {
    val inst = PartiallyImplementedClass()

    assertNotImplemented { inst.prop }
    assertNotImplemented { inst.method1() }
    assertNotImplemented { inst.method2() }
    assertNotImplemented { inst.method4() }
    assertNotImplementedWithMessage("what if false") { inst.method3(false, "test") }
    assertNotImplementedWithMessage("write message") { inst.method3(true, "t") }
    assertEquals("test", inst.method3(true, "test"))
}
