// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class Test {
    private abstract class Base {
        protected fun duplicate(s: String) = s + "K"

        protected inline fun doInline(block: () -> String): String {
            return duplicate(block())
        }
    }

    private class Extender: Base() {
        fun doSomething(): String {
            return doInline { "O" }
        }
    }

    fun run(): String {
        return Extender().doSomething();
    }
}

// FILE: 2.kt

import test.*

fun box() : String {
    return Test().run()
}
