// WITH_STDLIB
// CHECK_BYTECODE_LISTING

class A {
    var x = "start"
}

val a: Any = A()

class B {
    var y by (a as A)::x
}

fun box(): String {
    val b = B()
    if (b.y != "start") return "FAIL 1: ${b.y}"
    b.y = "K"
    return "O" + (a as A).x
}
