import kotlin.test.Test
import kotlin.test.assertTrue

private const val GREETING = "Hello, Kotlin/Native!"

class HelloTest2 {
    @Test
    fun testHello() {
        assertTrue("Kotlin/Native" in GREETING)
    }
}
