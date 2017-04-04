import kotlin.test.*



fun box() {
    assertEquals("A", "A".capitalize())
    assertEquals("A", "a".capitalize())
    assertEquals("Abcd", "abcd".capitalize())
    assertEquals("Abcd", "Abcd".capitalize())
}
