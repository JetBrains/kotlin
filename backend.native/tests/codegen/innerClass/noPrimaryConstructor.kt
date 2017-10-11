package codegen.innerClass.noPrimaryConstructor

import kotlin.test.*

class Outer(val s: String) {
    inner class Inner {
        constructor(x: Int) {
            this.x = x
        }

        constructor(z: String) {
            x = z.length
        }

        val x: Int

        fun foo() = s
    }

}

@Test fun runTest() {
    println(Outer("OK").Inner(42).foo())
    println(Outer("OK").Inner("zzz").foo())
}