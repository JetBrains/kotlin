// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: 1.kt

package test2

import test.A

public fun box(): String {
    return B().test(B())
}

public class B : A() {
    public fun test(other:Any): String {
        if (other is B && other.s == 2) {
            return "OK"
        }
        return "fail"
    }
}

// FILE: 2.kt

package test

open class A {
    @JvmField protected val s = 2;
}
