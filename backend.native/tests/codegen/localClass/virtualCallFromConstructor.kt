package codegen.localClass.virtualCallFromConstructor

import kotlin.test.*

abstract class WaitFor {
    init {
        condition()
    }

    abstract fun condition(): Boolean;
}

fun box(): String {
    val local = ""
    var result = "fail"
    val s = object : WaitFor() {

        override fun condition(): Boolean {
            result = "OK"
            return result.length == 2
        }
    }

    return result;
}

@Test fun runTest() {
    println(box())
}