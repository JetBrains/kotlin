// SKIP_KT_DUMP

class X {
    var value = ""

    operator fun plusAssign(data: String) {
        value += data
    }
}

abstract class A {
    lateinit var x: X
        private set

    var y: X = X(); private set
}

class B : A()

fun test(b: B) {
    b.x += "x"
    b.y += "y"
}