// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: 1.kt

package test2

import test.A

class C : A() {
    fun a(): String {
        return this.s
    }
}

public fun box(): String {
    return C().a()
}

// FILE: 2.kt

package test

open class A {
    @JvmField protected val s = "OK";
}
