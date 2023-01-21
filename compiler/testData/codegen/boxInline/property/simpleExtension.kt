// FILE: 1.kt
package test

var value: Int = 0

inline var Int.z: Int
    get() = this + ++value
    set(p: Int) { value = p + this}

// FILE: 2.kt
import test.*

fun box(): String {
    val v = 11.z
    if (v != 12) return "fail 1: $v"

    11.z = v + 2

    if (value != 25) return "fail 2: $value"
    var p = 11.z

    if (p != 37)  return "fail 3: $p"

    return "OK"
}
