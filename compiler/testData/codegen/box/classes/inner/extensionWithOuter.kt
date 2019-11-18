// IGNORE_BACKEND_FIR: JVM_IR
class Outer(val value: String) {

    inner class Inner {
        fun Outer.foo() = value
    }
}

fun Outer.Inner.test() = Outer("OK").foo()

fun box(): String {
    return Outer("Fail").Inner().test()
}