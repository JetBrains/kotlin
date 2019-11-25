// IGNORE_BACKEND_FIR: JVM_IR
// FILE: a.kt

package a

import b.*

class B {
    companion object : A() {}

    init {
        foo()
    }
}

fun box(): String {
    B()
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
