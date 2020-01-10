// FILE: Base.java

public class Base {
    protected String foo() { return ""; }
}

// FILE: O.kt

open class Wrapper(val b: Boolean)

object O {
    private class Derived(private val bar: Int) : Base() {
        private inner class Some(val z: Boolean) {
            fun test() {
                val x = bar
                val o = object : Wrapper(z) {
                    fun local() {
                        val y = foo()
                    }
                    val oo = object {
                        val zz = z
                    }
                }
            }
        }
        fun test() {
            val x = bar
            val o = object {
                fun local() {
                    val y = foo()
                }
            }
        }
    }
}

class Generator(val codegen: Any) {
    private fun gen(): Any =
        object : Wrapper(true) {
            private fun invokeFunction() {
                val c = codegen
                val cc = codegen.hashCode()
            }
        }
}