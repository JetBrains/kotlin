import kotlin.*
import kotlin.test.*


fun box() {
    var callCount = 0
    val lazyInt = lazy { ++callCount }

    assertEquals(0, callCount)
    assertFalse(lazyInt.isInitialized())
    assertEquals(1, lazyInt.value)
    assertEquals(1, callCount)
    assertTrue(lazyInt.isInitialized())

    lazyInt.value
    assertEquals(1, callCount)
}
