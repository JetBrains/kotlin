// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

open class A {
    open fun test() = "OK"
}

object X : A() {
    override fun test(): String {
        return "fail"
    }

    inline fun doTest(): String {
        return super.test()
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return X.doTest()
}
