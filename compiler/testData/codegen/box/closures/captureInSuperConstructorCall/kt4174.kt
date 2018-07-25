// IGNORE_BACKEND: JVM_IR
open class C(val f: () -> String)

class B(val x: String) {
    fun foo(): C {
        class A : C({x}) {}
        return A()
    }
}

fun box() = B("OK").foo().f()