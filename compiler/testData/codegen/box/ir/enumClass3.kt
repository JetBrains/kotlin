// WITH_STDLIB
import kotlin.test.assertEquals

enum class TestEnumClass {
    ZERO {
        override fun describe() = "nothing"
    };
    abstract fun describe(): String
}

fun box(): String {
    assertEquals(TestEnumClass.ZERO.describe(), "nothing")
    return "OK"
}