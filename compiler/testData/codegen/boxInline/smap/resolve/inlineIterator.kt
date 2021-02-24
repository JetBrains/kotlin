// FILE: 1.kt

package zzz

public class A(val p: Int)

operator inline fun A.iterator() = (1..p).iterator()

// FILE: 2.kt

import zzz.*

fun box(): String {
    var p = 0
    for (i in A(5)) {
        p += i
    }

    return if (p == 15) "OK" else "fail: $p"
}
