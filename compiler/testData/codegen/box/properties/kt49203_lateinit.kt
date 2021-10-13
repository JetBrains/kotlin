class X {
    var value = ""

    operator fun plusAssign(data: String) {
        value += data
    }
}

abstract class A {
    lateinit var x: X
        private set

    fun init() {
        x = X()
    }
}

class B : A()

fun box(): String {
    val a = B()
    a.init()
    a.x += "OK"
    return a.x.value
}