// KT-66103: companion object is not initialized
// WITH_STDLIB
import kotlin.test.*

val sb = StringBuilder()

class TestClass {
    companion object {
        init {
            sb.append("OK")
        }
    }
}

fun box(): String {
    val t1 = TestClass()
    val t2 = TestClass()

    return sb.toString()
}
