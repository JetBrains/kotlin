// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

interface Run {
    fun run(): String
}

internal class A {
    inline fun doSomething(): Run  {
        return object : Run {
            override fun run(): String =  "OK"
        }
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return A().doSomething().run()
}
