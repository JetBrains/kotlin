// FILE: 1.kt
package test

var result = 1

inline var Int.z: Int
    get() = result
    set(value)  {
        result = value + this
    }

// FILE: 2.kt
import test.*

fun box(): String {
    1.z += 0
    if (result != 2) return "fail 1: $result"

    var p = 1.z++
    if (result != 4) return "fail 2: $result"
    if (p != 2) return "fail 3: $p"

    p = ++1.z
    if (result != 6) return "fail 4: $result"
    if (p != 6) return "fail 5: $p"

    return "OK"
}
