package codegen.localClass.noPrimaryConstructor

import kotlin.test.*

fun box(s: String): String {
    class Local {
        constructor(x: Int) {
            this.x = x
        }

        constructor(z: String) {
            x = z.length
        }

        val x: Int

        fun result() = s
    }

    return Local(42).result() + Local("zzz").result()
}

@Test fun runTest() {
    println(box("OK"))
}