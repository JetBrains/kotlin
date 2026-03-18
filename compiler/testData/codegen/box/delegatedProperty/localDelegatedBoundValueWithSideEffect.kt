var created: Int = 0

class A(val x: Int) {
    init { created += 1 }
}

class B(var x: Int) {
    init { created += 1000 }
}

fun create() = A(1)

fun box(): String {
    val t1 by A(1)::x
    var t2 by B(1)::x
    if (t1 != 1) return "FAIL 1: $t1"
    if (t2 != 1) return "FAIL 2: $t2"
    t2 = 3
    if (t2 != 3) return "FAIL 3: $t2"
    if (created != 1001) return "FAIL 4: $created != 1001"
    return "OK"
}