// FILE: 1.kt
package test

class A {
    var result = 1
    
    inline var z: Int
        get() = result
        set(value) {
            result = value
        }

}

// FILE: 2.kt
import test.*

fun box(): String {
    val a = A()
    a.z += 1
    if (a.z != 2) return "fail 1: $a.z"

    var p = a.z++
    if (a.z != 3) return "fail 2: $a.z"
    if (p != 2) return "fail 3: $p"

    p = ++a.z
    if (a.z != 4) return "fail 4: $a.z"
    if (p != 4) return "fail 5: $p"

    return "OK"
}