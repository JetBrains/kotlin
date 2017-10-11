package codegen.localClass.innerTakesCapturedFromOuter

import kotlin.test.*

fun box() {
    var previous: Any? = null
    for (i in 0 .. 2) {
        class Outer {
            inner class Inner {
                override fun toString() = i.toString()
            }

            override fun toString() = Inner().toString()
        }
        if (previous != null) println(previous.toString())
        previous = Outer()
    }
}

@Test fun runTest() {
    box()
}