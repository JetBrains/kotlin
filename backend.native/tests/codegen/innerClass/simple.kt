package codegen.innerClass.simple

import kotlin.test.*

class Outer {
    inner class Inner {
        fun box() = "OK"
    }
}

fun box() = Outer().Inner().box()

@Test fun runTest()
{
    println(box())
}