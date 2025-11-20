// WITH_STDLIB

import kotlin.test.*

class A {
    var field0:Int = 0;
    constructor(arg0:Int) {
        field0 = arg0
    }
}

fun box(): String {
    assertEquals(42, A(42).field0)
    return "OK"
}
