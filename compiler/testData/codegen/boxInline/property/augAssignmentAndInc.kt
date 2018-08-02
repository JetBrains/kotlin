// FILE: 1.kt
package test

var result = 1

inline var z: Int
    get() = result
    set(value)  {
        result = value
    }

// FILE: 2.kt
import test.*

fun box(): String {
    z += 1
    if (z != 2) return "fail 1: $z"

    var p = z++
    if (result != 3) return "fail 2: $result"
    if (p != 2) return "fail 3: $p"

    p = ++z
    if (result != 4) return "fail 4: $result"
    if (p != 4) return "fail 5: $p"

    return "OK"
}