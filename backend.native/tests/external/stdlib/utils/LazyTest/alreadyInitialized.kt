import kotlin.*
import kotlin.test.*


fun box() {
    val lazyInt = lazyOf(1)

    assertTrue(lazyInt.isInitialized())
    assertEquals(1, lazyInt.value)
}
