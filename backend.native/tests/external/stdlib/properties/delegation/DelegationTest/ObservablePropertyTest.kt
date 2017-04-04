import kotlin.test.*

import kotlin.properties.*

class ObservablePropertyTest {
    var result = false

    var b: Int by Delegates.observable(1, { property, old, new ->
        assertEquals("b", property.name)
        result = true
        assertEquals(new, b, "New value has already been set")
    })

    fun doTest() {
        b = 4
        assertTrue(b == 4, "fail: b != 4")
        assertTrue(result, "fail: result should be true")
    }
}

fun box() {
    ObservablePropertyTest().doTest()
}