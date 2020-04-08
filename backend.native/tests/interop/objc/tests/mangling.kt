import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testMangling() {
    assertEquals(11, myStruct.`Companion$`)
    assertEquals(12, myStruct._Companion)
    assertEquals(13, myStruct.`$_Companion`)
    assertEquals(14, myStruct.`super`)

    val objc = FooMangled()
    objc.`Companion$` = 99
    assertEquals(99, objc.Companion())
    assertEquals(99, objc.`Companion$`)

    enumMangledStruct.smth = Companion
    assertEquals(Companion, enumMangledStruct.smth)
}