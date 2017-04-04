import kotlin.test.*


fun box() {
    // this test is needed for JS implementation
    assertEquals("em", buildString {
        append("element", 2, 4)
    })
}
