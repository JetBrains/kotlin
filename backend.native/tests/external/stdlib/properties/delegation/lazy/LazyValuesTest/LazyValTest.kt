import kotlin.test.*
import kotlin.properties.*

class LazyValTest {
    var result = 0
    val a by lazy {
        ++result
    }

    fun doTest() {
        a
        assertTrue(a == 1, "fail: initializer should be invoked only once")
    }
}

fun box() {
    LazyValTest().doTest()
}