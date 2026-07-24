// WITH_STDLIB

class D {
    var y: String = "initial"
}

class A

class C {
    val d = D()
    var A.x: String by d::y
}

fun box(): String {
    val a = A()
    with(C()) {
        a.x = "OK"
        return a.x
    }
}
