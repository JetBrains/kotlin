// !LANGUAGE: -ProhibitOpenValDeferredInitialization
abstract class A {
    val b = B("O")

    open val c: B

    val d: B
        get() = field

    var e: String
        get() = field

    init {
        c = B("O")
        d = B("O")
        e = "O"

        b += ","
        c += "."
        d += ";"
        e += "|"
    }
}

class B(var value: String) {
    operator fun plusAssign(o: String) {
        value += o
    }
}

class C : A() {
    init {
        b += "K"
        c += "K"
        d += "K"
        e += "K"
    }
}

fun box(): String {
    val c = C()
    val result = "${c.b.value} ${c.c.value} ${c.d.value} ${c.e}"
    if (result != "O,K O.K O;K O|K") return "fail: $result"

    return "OK"
}