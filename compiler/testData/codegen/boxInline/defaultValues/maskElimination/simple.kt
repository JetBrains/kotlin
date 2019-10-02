// FILE: 1.kt

package test

fun calc() = "OK"

/*open modifier for method handle check in default method*/
open class A {
    inline fun test(p: String = calc()): String {
        return p
    }

    inline fun String.testExt(p: String = "K"): String {
        return this + p
    }

    fun callExt(): String {
        return "O".testExt()
    }

    fun callExt(arg: String): String {
        return "O".testExt(arg)
    }
}

// FILE: 2.kt

import test.*

fun box() : String {
    if (A().callExt() != "OK") return "fail 1: ${A().callExt()}"
    if (A().callExt("O") != "OO") return "fail 2: ${A().callExt("O")}"
    if (A().test("KK") != "KK") return "fail 3: ${A().test("KK")}"

    return A().test()
}