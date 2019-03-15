// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

var res = 1

class A {

    inline operator fun Int.get(z: Int, p: () -> Int, defaultt: Int = 100) = this + z + p() + defaultt

    inline operator fun Int.set(z: Int, p: () -> Int, l: Int/*, x : Int = 1000*/) {
        res = this + z + p() + l /*+ x*/
    }
}

// FILE: 2.kt

import test.*


fun box(): String {

    val z = 1;

    with(A()) {

        val p = z[2, { 3 }]
        if (p != 106) return "fail 1: $p"

        val captured = 3;
        z[2, { captured }] = p
        if (res != 112) return "fail 2: $res"
    }

    return "OK"
}
