// WITH_STDLIB

interface I {
    var z: String
}

class X {
    var p: String = "Fail"
}

class A {
    val x = X()

    inner class Y : I {
        override var z: String by x::p
    }

    val y = Y()
}

fun box(): String {
    val a = A()
    a.y.z = "OK"
    return a.y.z
}
