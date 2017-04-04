import kotlin.*
import kotlin.test.*


fun box() {
    var callCount = 0
    val lazyInt = lazy { ++callCount }

    assertNotEquals("1", lazyInt.toString())
    assertEquals(0, callCount)

    assertEquals(1, lazyInt.value)
    assertEquals("1", lazyInt.toString())
    assertEquals(1, callCount)
}
