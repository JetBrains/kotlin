// WITH_STDLIB

class A {
    var x: String = "Fail"
}

class C {
    var A.y: String by A::x
}

fun box(): String {
    val a = A()
    C().apply {
        a.y = "OK"
        return a.y
    }
}
