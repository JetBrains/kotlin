// WITH_STDLIB
// FILE: 1.kt
package test

var res = 1

class A {

    inline operator fun Int.get(z: Int, p: Int) = this + z + p

    inline operator fun Int.set(z: Int, p: Int, l: Int) {
        res = this + z + p + l
    }

}

// FILE: 2.kt

import test.*


fun box(): String {

    with(A()) {
        val z = 1;

        val p = z[2, 3]
        if (p != 6) return "fail 1: $p"

        z[2, 3] = p
        if (res != 12) return "fail 2: $res"
    }

    return "OK"
}
