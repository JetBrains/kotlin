// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

var order = StringBuilder()

inline fun expectOrder(at: String, expected: String, body: () -> Unit) {
    order = StringBuilder() // have to do that in order to run this test in JS
    body()
    if (order.toString() != expected) {
        throw AssertionError("$at: expected: '$expected', actual: '$order'")
    }
}

class Z(val x: Int) : Comparable<Z> {
    override fun compareTo(other: Z): Int {
        order.append("c:$x,${other.x} ")
        return x.compareTo(other.x)
    }
}

fun z(i: Int): Z {
    order.append("z:$i ")
    return Z(i)
}

fun zr(i: Int, j: Int) = z(i) .. z(j)

fun box(): String {
    expectOrder("#1",  "z:1 z:3 z:0 c:0,1 ") { z(0) in z(1) .. z(3) }
    expectOrder("#1a", "z:1 z:3 z:0 c:0,1 ") { z(0) in zr(1, 3) }

    expectOrder("#2",  "z:1 z:3 z:2 c:2,1 c:2,3 ") { z(2) in z(1) .. z(3) }
    expectOrder("#2a", "z:1 z:3 z:2 c:2,1 c:2,3 ") { z(2) in zr(1, 3) }

    expectOrder("#3",  "z:1 z:3 z:4 c:4,1 c:4,3 ") { z(4) in z(1) .. z(3) }
    expectOrder("#3a", "z:1 z:3 z:4 c:4,1 c:4,3 ") { z(4) in zr(1, 3) }

    expectOrder("#4",  "z:1 z:3 z:0 c:0,1 ") { z(0) !in z(1) .. z(3) }
    expectOrder("#4a", "z:1 z:3 z:0 c:0,1 ") { z(0) !in zr(1, 3) }

    expectOrder("#5",  "z:1 z:3 z:2 c:2,1 c:2,3 ") { z(2) !in z(1) .. z(3) }
    expectOrder("#5a", "z:1 z:3 z:2 c:2,1 c:2,3 ") { z(2) !in zr(1, 3) }

    expectOrder("#6",  "z:1 z:3 z:4 c:4,1 c:4,3 ") { z(4) !in z(1) .. z(3) }
    expectOrder("#6a", "z:1 z:3 z:4 c:4,1 c:4,3 ") { z(4) !in zr(1, 3) }

    return "OK"
}