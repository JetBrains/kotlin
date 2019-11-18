// IGNORE_BACKEND_FIR: JVM_IR
//KT-3927 Inner class cannot be instantiated with child instance of outer class

abstract class Base {
    inner class Inner {
        fun o() = "O"
        fun k() = "K"
    }
}

class Child : Base()

fun box(): String {
    var result = ""
    result += Child().Inner().o()

    fun Child.f() {
        result += Inner().k()
    }
    Child().f()

    return result
}

