// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JS
// FILE: 1.kt

package test

class Person(val name: String) {

    fun sayName() = doSayName { name }

    inline fun doSayName(crossinline call: () -> String): String {
        return nestedSayName1 { nestedSayName2 { call() } }
    }

    fun nestedSayName1(call: () -> String) = call()

    inline fun nestedSayName2(call: () -> String)  = call()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return Person("OK").sayName()
}
