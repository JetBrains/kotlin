// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

public class Data(val value: Int)

public class Input(val d: Data)  {
    public fun data() : Int = 100
}

public inline fun <R> use(block: ()-> R) : R {
    return block()
}

// FILE: 2.kt

import test.*

fun test1(d: Data): Int {
    val input = Input(d)
    var result = 10
    with(input) {
        fun localFun() {
            result = input.d.value
        }
        localFun()
    }
    return result
}


fun box(): String {
    val result = test1(Data(11))
    if (result != 11) return "test1: ${result}"

    return "OK"
}
