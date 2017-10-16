package codegen.innerClass.secondaryConstructor

import kotlin.test.*

class Outer(val x: Int) {
    inner class Inner() {
        inner class InnerInner() {

            init {
                println(x)
            }

            lateinit var s: String

            constructor(s: String) : this() {
                this.s = s
            }
        }
    }
}

@Test fun runTest() {
    Outer(42).Inner().InnerInner("zzz")
}
