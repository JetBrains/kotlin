// WITH_STDLIB

interface I {
    var z: String
}

class X {
    var p: String = "Fail"
}

class A {
    val x = X()

    val y = object : I {
        override var z: String by x::p
    }
}

fun box(): String {
    val a = A()
    a.y.z = "OK"
    return a.y.z
}