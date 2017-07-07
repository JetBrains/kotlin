// WITH_RUNTIME

val order = StringBuilder()

inline fun expectOrder(at: String, expected: String, body: () -> Unit) {
    order.setLength(0)
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

fun box(): String {
    expectOrder("z0 in z1 .. z3", "z:1 z:3 z:0 c:1,0 ") { z(0) in z(1) .. z(3) }
    expectOrder("z2 in z1 .. z3", "z:1 z:3 z:2 c:1,2 c:3,2 ") { z(2) in z(1) .. z(3) }
    expectOrder("z4 in z1 .. z3", "z:1 z:4 z:2 c:1,2 c:4,2 ") { z(2) in z(1) .. z(4) }

    expectOrder("z0 !in z1 .. z3", "z:1 z:3 z:0 c:1,0 ") { z(0) !in z(1) .. z(3) }
    expectOrder("z2 !in z1 .. z3", "z:1 z:3 z:2 c:1,2 c:3,2 ") { z(2) !in z(1) .. z(3) }
    expectOrder("z4 !in z1 .. z3", "z:1 z:4 z:2 c:1,2 c:4,2 ") { z(2) !in z(1) .. z(4) }

    return "OK"
}