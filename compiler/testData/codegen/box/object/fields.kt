// WITH_STDLIB

import kotlin.test.*

private var globalValue = 1
var global:Int
    get() = globalValue
    set(value:Int) {globalValue = value}

fun globalTest(i:Int):Int {
    global += i
    return global
}


fun box(): String {
    assertEquals(1, global)
    assertEquals(42, globalTest(41))
    assertEquals(42, global)

    return "OK"
}
