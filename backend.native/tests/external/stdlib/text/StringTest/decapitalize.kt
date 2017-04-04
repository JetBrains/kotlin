import kotlin.test.*



fun box() {
    assertEquals("a", "A".decapitalize())
    assertEquals("a", "a".decapitalize())
    assertEquals("abcd", "abcd".decapitalize())
    assertEquals("abcd", "Abcd".decapitalize())
    assertEquals("uRL", "URL".decapitalize())
}
