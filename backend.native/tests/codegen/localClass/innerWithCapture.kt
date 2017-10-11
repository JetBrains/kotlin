package codegen.localClass.innerWithCapture

import kotlin.test.*

fun box(s: String): String {
    class Local {
        open inner class Inner() {
            open fun result() = s
        }
    }

    return Local().Inner().result()
}

@Test fun runTest() {
    println(box("OK"))
}