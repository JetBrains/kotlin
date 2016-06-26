// FILE: 1.kt

package test

object Host {
    // private final foo()V
    inline fun <reified T> foo(): String {
        return "OK"
    }
}


// FILE: 2.kt

import test.Host.foo

fun box(): String {
    return foo<Any>()
}
