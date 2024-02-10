// IGNORE_INLINER_K2: IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

class A {
    val param = "start"
    var result = "fail"
    var addParam = "_additional_"

    inline fun inlineFun(arg: String, crossinline f: (String) -> Unit) {
        {
            f(arg + addParam)
        }.let { it() }
    }


    fun box(): String {
        {
            inlineFun("2") { a ->
                {
                    result = param + a
                }.let { it() }
            }
        }.let { it() }
        return if (result == "start2_additional_") "OK" else "fail: $result"
    }

}

// FILE: 2.kt

import test.*

fun box(): String {
    return A().box()
}
