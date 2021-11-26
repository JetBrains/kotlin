class X {
    var value = ""

    operator fun plusAssign(data: String) {
        value += data
    }
}

abstract class A {
    var x: X = X()
        private set
}

class B : A()

fun box(): String {
    val a = B()
    a.x += "OK"
    return a.x.value
}