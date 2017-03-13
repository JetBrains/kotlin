// FILE: 1.kt

package test

inline fun emptyFun(arg: String = "O") {

}

inline fun simpleFun(arg: String = "O"): String {
    val r = arg;
    return r;
}


inline fun simpleDoubleFun(arg: Double = 1.0): Double {
    val r = arg + 1;
    return r;
}

// FILE: 2.kt

import test.*

fun testCompilation(): String {
    emptyFun()
    emptyFun("K")

    return "OK"
}

fun simple(): String {
    return simpleFun() + simpleFun("K")
}

fun box(): String {
    var result = testCompilation()
    if (result != "OK") return "fail1: ${result}"

    result = simple()
    if (result != "OK") return "fail2: ${result}"

    var result2 = simpleDoubleFun(2.0)
    if (result2 != 2.0 + 1.0) return "fail3: ${result2}"

    return "OK"
}
