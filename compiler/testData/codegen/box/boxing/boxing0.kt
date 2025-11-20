// WITH_STDLIB

import kotlin.test.*

class Box<T>(t: T) {
    var value = t
}

fun box(): String {
    val box: Box<Int> = Box<Int>(17)
    if (box.value != 17) return "FAIL 1: ${box.value}"

    val nonConst = 17
    val box2: Box<Int> = Box<Int>(nonConst)
    if (box2.value != 17) return "FAIL 1: ${box2.value}"

    return "OK"
}
