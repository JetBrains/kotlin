package codegen.innerClass.generic

import kotlin.test.*

class Outer {
    inner class Inner<T>(val t: T) {
        fun box() = t
    }
}

fun box(): String {
    if (Outer().Inner("OK").box() != "OK") return "Fail"
    val x: Outer.Inner<String> = Outer().Inner("OK")
    return x.box()
}

@Test fun runTest() {
    println(box())
}