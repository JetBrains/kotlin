// There's no String?.plus implementation in JS stdlib
// IGNORE_BACKEND: JS

// FILE: 1.kt

package test

public class Z(public val value: Int = 0) {

    inline public fun run(p1: String? = null): String? {
        return p1 + value
    }


    inline public fun run(p1: String = "", lambda: (a: String, b: Int) -> String, p2: Int = 0): String {
        return lambda(p1, p2)
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    if (Z().run() != "null0") return "fail 1: ${Z().run()}"

    if (Z().run("OK") != "OK0") return "fail 2"

    if (Z().run("OK", { a, b -> a + b }, 1) != "OK1") return "fail 3"

    if (Z().run(lambda = { a: String, b: Int -> a + b }) != "0") return "fail 4"

    return "OK"
}
