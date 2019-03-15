// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

fun a(b: () -> String) : String {
    return b()
}

inline fun test(l: () -> String): String {
    return l()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING

import test.*

fun test1() : String {
    return a {
        test {
            return@a "OK"
        }
    }
}

fun test2() : String {
    return test z@ {
        return@z "OK"
    }
}

fun test3() : String {
    return test {
        return@test "OK"
    }
}

fun test4() : String {
    return a z@ {
        test {
            return@z "OK"
        }
    }
}



fun box() : String {
    if (test1() != "OK") return "fail 1: ${test1()}"

    if (test2() != "OK") return "fail 2: ${test2()}"

    if (test3() != "OK") return "fail 3: ${test3()}"

    if (test4() != "OK") return "fail 4: ${test4()}"

    return "OK"
}
