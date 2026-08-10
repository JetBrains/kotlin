// WITH_STDLIB
// CHECK_BYTECODE_LISTING

class A {
    var x = "start"
}

fun test(a: Any): String {
    if (a !is A) return "FAIL 0: $a"
    class B {
        var y by a::x
    }
    val b = B()
    if (b.y != "start") return "FAIL 1: ${b.y}"
    b.y = "K"
    return "O" + a.x
}

fun box(): String = test(A())
