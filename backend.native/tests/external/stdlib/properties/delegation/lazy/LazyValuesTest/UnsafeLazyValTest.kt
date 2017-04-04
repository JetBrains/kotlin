import kotlin.test.*
import kotlin.properties.*

class UnsafeLazyValTest {
    var result = 0
    val a by lazy(LazyThreadSafetyMode.NONE) {
        ++result
    }

    fun doTest() {
        a
        assertTrue(a == 1, "fail: initializer should be invoked only once")
    }
}

fun box() {
    UnsafeLazyValTest().doTest()
}