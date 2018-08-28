//WITH_RUNTIME

import kotlin.test.assertEquals

enum class Test {
    OK
}

fun box(): String {
    assertEquals(Test.OK.ordinal, 0)
    assertEquals(Test.OK.name, "OK")
    
    return "OK"
}