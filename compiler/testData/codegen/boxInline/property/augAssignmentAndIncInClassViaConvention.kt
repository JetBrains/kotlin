// FILE: 1.kt
package test

class Test(var result: Int)

class A {
    var result = Test(1)

    inline var z: Test
        get() = result
        set(value) {
            result = value
        }
}

operator fun Test.plus(p: Int): Test {
    return Test(result + p)
}

operator fun Test.inc(): Test {
    return Test(result + 1)
}

// FILE: 2.kt
import test.*

fun box(): String {
    val a = A()
    a.z = Test(1)
    a.z += 1
    if (a.result.result != 2) return "fail 1: ${a.result.result}"

    var p = a.z++
    if (a.result.result != 3) return "fail 2: ${a.result.result}"
    if (p.result != 2) return "fail 3: ${p.result}"

    p = ++a.z
    if (a.result.result != 4) return "fail 4: ${a.result.result}"
    if (p.result != 4) return "fail 5: ${p.result}"

    return "OK"
}