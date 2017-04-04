import kotlin.test.*


fun box() {
    assertTrue(KotlinVersion.CURRENT.isAtLeast(1, 1))
    assertTrue(KotlinVersion.CURRENT.isAtLeast(1, 1, 0))
    assertTrue(KotlinVersion.CURRENT >= KotlinVersion(1, 1))
    assertTrue(KotlinVersion(1, 1) <= KotlinVersion.CURRENT)

    val anotherCurrent = KotlinVersion.CURRENT.run { KotlinVersion(major, minor, patch) }
    assertEquals(KotlinVersion.CURRENT, anotherCurrent)
    assertEquals(KotlinVersion.CURRENT.hashCode(), anotherCurrent.hashCode())
    assertEquals(0, KotlinVersion.CURRENT.compareTo(anotherCurrent))
}
