//WITH_RUNTIME
//IGNORE_BACKEND: JS_IR
import kotlin.test.assertEquals

enum class TestEnumClass(val x: Int) {
    ZERO
//    {
//        init {
//        }
//    }
    ;
    constructor(): this(0)
}

fun box(): String {
    assertEquals(TestEnumClass.ZERO.x, 0)

    return "OK"
}