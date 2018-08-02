// FILE: 1.kt

package test


inline fun <T> simpleFun(arg: String = "O", lambda: (String) -> T): T {
    return lambda(arg)
}


inline fun <T> simpleFunR(lambda: (String) -> T, arg: String = "O"): T {
    return lambda(arg)
}

// FILE: 2.kt

import test.*

fun simple(): String {
    val k = "K"
    return simpleFun(lambda = {it + "O"}) + simpleFun("K", {k + it})
}

fun simpleR(): String {
    val k = "K"
    return simpleFunR({it + "O"}) + simpleFunR({k + it}, "K")
}

fun box(): String {

    var result = simple()
    if (result != "OOKK") return "fail1: ${result}"

    result = simpleR()
    if (result != "OOKK") return "fail2: ${result}"

    return "OK"
}
