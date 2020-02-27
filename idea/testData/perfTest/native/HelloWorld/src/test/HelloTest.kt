import kotlin.test.Test
import kotlin.test.assertTrue

private const val GREETING = "Hello, Kotlin/Native!"

class HelloTest {
    @Test
    fun testHello() {
        assertTrue("Kotlin/Native" in GREETING)
    }
}
