package codegen.boxing.boxing0

import kotlin.test.*

class Box<T>(t: T) {
    var value = t
}

@Test fun runTest() {
    val box: Box<Int> = Box<Int>(17)
    println(box.value)
}

