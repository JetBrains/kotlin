// WITH_STDLIB
// FILE: 1.kt

package test

enum class MyEnum {
    K;

    //TODO: KT-4693
    inline fun <T> doSmth(a: T) : String {
        return a.toString() + K.name
    }
}

// FILE: 2.kt

import test.*

fun test1(): String {
    return MyEnum.K.doSmth("O")
}

fun box(): String {
    val result = test1()
    if (result != "OK") return "fail1: ${result}"

    return "OK"
}
