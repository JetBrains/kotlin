// IGNORE_BACKEND_FIR: JVM_IR
// FILE: a.kt

package a

import b.*

interface B {
    companion object : A() {}

    fun test() {
        foo()
    }
}

class C : B

fun box(): String {
    C().test()
    return result
}

// FILE: b.kt

package b

var result = "fail"

abstract class A {
    protected fun foo() {
        result = "OK"
    }
}
