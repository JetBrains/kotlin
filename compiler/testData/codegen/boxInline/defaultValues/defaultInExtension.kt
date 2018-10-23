// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

inline public fun String.run(p1: String? = null): String {
    return this + p1
}

inline public fun String.run(p1: String = "", lambda: (a: String, b: Int) -> String, p2: Int = 0): String {
    return lambda(p1, p2) + this
}

public class Z(val value: Int = 0) {

    inline public fun String.run(p1: String? = null): String? {
        return this + p1
    }

    inline public fun String.run(p1: String = "", lambda: (a: String, b: Int) -> String, p2: Int = 0): String {
        return lambda(p1, p2) + this
    }

}

// FILE: 2.kt

import test.*

fun testExtensionInClass() : String {

    var res = with(Z(1)) { "1".run("OK") }
    if (res != "1OK") return "failed in class 1: $res"

    res = with(Z(1)) { "1".run() }
    if (res != "1null") return "failed in class 2: $res"

    res = with(Z(2)) { "3".run("OK", { a, b -> a + b + value }, 1) }
    if (res != "OK123") return "failed in class 3: $res"

    res = with(Z(3)) { "4".run(lambda = { a, b -> a + b + value }) }
    if (res != "034") return "failed in class 4: $res"

    return "OK"
}

fun box(): String {

    var res = "1".run("OK")
    if (res != "1OK") return "failed 1: $res"

    res = "1".run()
    if (res != "1null") return "failed 2: $res"

    res = "3".run("OK", { a, b -> a + b}, 1)
    if (res != "OK13") return "failed 3: $res"

    res = "4".run(lambda = { a, b -> a + b})
    if (res != "04") return "failed 4: $res"

    return testExtensionInClass()
}
