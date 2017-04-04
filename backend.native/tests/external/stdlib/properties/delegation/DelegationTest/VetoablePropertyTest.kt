import kotlin.test.*

import kotlin.properties.*

import kotlin.test.*

import kotlin.properties.*

class A(val p: Boolean)

class VetoablePropertyTest {
    var result = false
    var b: A by Delegates.vetoable(A(true), { property, old, new ->
        assertEquals("b", property.name)
        assertEquals(old, b, "New value hasn't been set yet")
        result = new.p == true;
        result
    })

    fun doTest() {
        val firstValue = A(true)
        b = firstValue
        assertTrue(b == firstValue, "fail1: b should be firstValue = A(true)")
        assertTrue(result, "fail2: result should be true")
        b = A(false)
        assertTrue(b == firstValue, "fail3: b should be firstValue = A(true)")
        assertFalse(result, "fail4: result should be false")
    }
}

fun box() {
    VetoablePropertyTest().doTest()
}
